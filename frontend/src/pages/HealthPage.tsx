import { useEffect, useState } from "react";

export function HealthPage() {
  const [status, setStatus] = useState<string>("…");

  useEffect(() => {
    fetch("/api/v1/health")
      .then((r) => r.json())
      .then((j) => setStatus(j.status ?? JSON.stringify(j)))
      .catch((e) => setStatus(String(e)));
  }, []);

  return (
    <section>
      <h1>Backend health</h1>
      <p>
        Статус: <strong>{status}</strong>
      </p>
    </section>
  );
}
