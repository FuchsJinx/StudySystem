import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "./client";
import { session } from "./session";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("api client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    session.clear();
  });

  it("adds bearer token and parses JSON", async () => {
    session.setToken("test-token");
    const fetchMock = vi.fn(async () => jsonResponse({ hello: "world" }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await api<{ hello: string }>("/demo");

    expect(result.hello).toBe("world");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/v1/demo");
    expect((init.headers as Record<string, string>).Authorization).toBe("Bearer test-token");
  });

  it("maps error payload to ApiError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(
          { error: { code: "BUDGET_EXCEEDED", message: "Бюджет исчерпан", details: ["group"] } },
          400,
        ),
      ),
    );

    await expect(api("/activities")).rejects.toMatchObject({
      name: "ApiError",
      status: 400,
      code: "BUDGET_EXCEEDED",
      message: "Бюджет исчерпан",
      details: ["group"],
    });
  });

  it("clears session and emits event on 401", async () => {
    session.setToken("expired");
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse({ error: { message: "Нет" } }, 401)));
    const listener = vi.fn();
    window.addEventListener("unirate:unauthorized", listener);

    await expect(api("/tests")).rejects.toBeInstanceOf(ApiError);
    expect(session.getToken()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
    window.removeEventListener("unirate:unauthorized", listener);
  });

  it("sends JSON body with POST", async () => {
    const fetchMock = vi.fn(async () => jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await api("/auth/login", { method: "POST", body: { email: "a@b.c" } });

    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(init.method).toBe("POST");
    expect(init.body).toBe(JSON.stringify({ email: "a@b.c" }));
    expect((init.headers as Record<string, string>)["Content-Type"]).toBe("application/json");
  });
});
