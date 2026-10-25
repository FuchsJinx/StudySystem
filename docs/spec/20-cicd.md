# 20 — CI/CD

## CI (каждый push / PR)

Файл: `.github/workflows/ci.yml`

| Job | Что делает |
|-----|------------|
| `backend` | `mvn test` + `package` на JDK 21, Postgres service |
| `frontend` | `npm install`, `tsc`, `vite build`, vitest |
| `docker` | `docker compose build` → `up` → curl `/api/v1/health` |

## CD (теги `v*`)

Файл: `.github/workflows/cd.yml`

- Сборка и push образов в **GHCR**:
  - `ghcr.io/<owner>/unirate-backend:<tag>`
  - `ghcr.io/<owner>/unirate-frontend:<tag>`
- Выкат на VPS: `docker compose pull && docker compose up -d` (вручную или отдельный runner).

## Локально

```bash
docker compose up --build
# Mailhog UI: http://localhost:8025
```
