import { session } from "./session";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: string[];

  constructor(status: number, code: string, message: string, details: string[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

interface ErrorPayload {
  error?: {
    code?: string;
    message?: string;
    details?: string[];
  };
}

export interface RequestOptions {
  method?: string;
  body?: unknown;
  form?: FormData;
  signal?: AbortSignal;
}

const API_BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? "/api/v1";

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    Accept: "application/json",
  };
  const token = session.getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  let body: BodyInit | undefined;
  if (options.form) {
    body = options.form;
  } else if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? (body ? "POST" : "GET"),
    headers,
    body,
    signal: options.signal,
  });

  if (!response.ok) {
    let payload: ErrorPayload = {};
    try {
      payload = (await response.json()) as ErrorPayload;
    } catch {
      // Non-JSON error (proxy/nginx).
    }
    const code = payload.error?.code ?? "UNKNOWN_ERROR";
    const message = payload.error?.message ?? `Ошибка HTTP ${response.status}`;
    if (response.status === 401 && token && !path.startsWith("/auth/login")) {
      session.clear();
      window.dispatchEvent(new CustomEvent("unirate:unauthorized"));
    }
    throw new ApiError(response.status, code, message, payload.error?.details ?? []);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    return (await response.text()) as T;
  }
  return (await response.json()) as T;
}

export async function downloadFile(path: string, filename: string): Promise<void> {
  const token = session.getToken();
  const response = await fetch(`${API_BASE}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    throw new ApiError(response.status, "DOWNLOAD_FAILED", "Не удалось скачать файл");
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
