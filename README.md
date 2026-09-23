# SmartBancs App — Reto Técnico NextGen Engineers

Microservicio de transferencias bancarias construido como parte del reto técnico "SmartBancs App". Recibe transacciones financieras vía API REST, aplica bloqueo pesimista ordenado para evitar deadlocks en transferencias concurrentes, garantiza idempotencia, y dispara de forma asíncrona un análisis de IA (mock) sin afectar el tiempo de respuesta de la transacción.

## Arquitectura

- **transactions-service**: microservicio Java 21 / Spring Boot 4.1.1 que expone `POST /api/transactions`. Usa Spring Data JPA + Flyway para persistencia, bloqueo pesimista (`SELECT ... FOR UPDATE`) para concurrencia segura, y un `AiRecommendationService` asíncrono (`@Async`) que simula una llamada a un servicio de IA externo sin bloquear la respuesta HTTP.
- **PostgreSQL 16**: base de datos relacional, levantada vía Docker Compose (`infra/docker-compose.yml`).
- **etl/**: script independiente en Python que simula la limpieza/transformación de un lote de datos transaccionales crudos antes de que sean consumidos para análisis o IA.

## Prerrequisitos

- Java 21
- Docker Desktop
- Python 3.10+ (solo para el script ETL)
- No se necesita Maven instalado: el proyecto incluye Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Cómo instalar y ejecutar

Todo el entorno (backend + base de datos) se levanta con un solo comando:
```
cd infra
docker compose up -d --build
```
El servicio queda disponible en `http://localhost:8080`. Flyway aplica las migraciones automáticamente al arrancar. La primera vez tarda un poco más porque construye la imagen del backend; las siguientes veces es casi inmediato.

**Sobre las credenciales de la base de datos:** `infra/docker-compose.yml` ya trae valores por defecto (`smartbancs`/`smartbancs`/`smartbancs`), así que el comando de arriba funciona sin ningún archivo adicional. Si prefieres usar tus propias credenciales, copia `infra/.env.example` a `infra/.env` y edítalo:
```
cp infra/.env.example infra/.env
```
`infra/.env` no se sube al repositorio (está en `.gitignore`); si existe, sus valores sobrescriben los defaults del compose.

Verifica que ambos contenedores estén sanos:
```
docker compose ps
curl.exe http://localhost:8080/actuator/health
```

**Alternativa para desarrollo activo del backend** (recarga más rápida al editar código, sin reconstruir la imagen):
1. Levantar solo PostgreSQL: `cd infra && docker compose up -d postgres`
2. Levantar el microservicio en el host:
   - Windows: `cd transactions-service && .\mvnw.cmd spring-boot:run`
   - Mac/Linux: `cd transactions-service && ./mvnw spring-boot:run`

## Cómo cargar datos de prueba

Con el contenedor de PostgreSQL corriendo, carga las cuentas de prueba de `infra/seed.sql`:

Mac/Linux:
```
docker exec -i smartbancs-postgres psql -U smartbancs -d smartbancs < infra/seed.sql
```

Windows PowerShell:
```
Get-Content infra/seed.sql | docker exec -i smartbancs-postgres psql -U smartbancs -d smartbancs
```

Esto crea 4 cuentas, incluida una con saldo bajo (`ACC1003`, saldo 15.00) para probar fácilmente el caso de saldo insuficiente. Para confirmar los IDs asignados:
```
docker exec smartbancs-postgres psql -U smartbancs -d smartbancs -c "SELECT id, account_number, balance FROM account ORDER BY id;"
```
En una base de datos recién creada, estas cuentas quedan típicamente con `id` 1 a 4 (en ese orden).

## Cómo probar

Ejemplos con `curl.exe` en PowerShell (ajusta los `sourceAccountId`/`destinationAccountId` a los IDs reales que obtuviste en el paso anterior).

**Transferencia exitosa:**
```
curl.exe -X POST http://localhost:8080/api/transactions -H "Content-Type: application/json" -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":100.00,"idempotencyKey":"demo-key-001"}'
```

**Idempotencia** (repite la misma petición con el mismo `idempotencyKey`; debe devolver la misma transacción sin duplicar el movimiento):
```
curl.exe -X POST http://localhost:8080/api/transactions -H "Content-Type: application/json" -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":100.00,"idempotencyKey":"demo-key-001"}'
```

**Error de saldo insuficiente** (usando la cuenta de saldo bajo como origen):
```
curl.exe -X POST http://localhost:8080/api/transactions -H "Content-Type: application/json" -d '{"sourceAccountId":3,"destinationAccountId":1,"amount":1000.00,"idempotencyKey":"demo-key-002"}'
```

Si al pegar alguno de estos comandos en PowerShell obtienes `{"error":"An unexpected error occurred","status":500}`, revisa que el cuerpo JSON haya llegado completo (a veces el copiar/pegar corta o altera comillas) — usa el comando de una sola línea tal cual está aquí, sin dividirlo en varias líneas con backtick.
Debe devolver `422 Unprocessable Entity` con el mensaje de saldo insuficiente.

Al revisar la consola del microservicio, cada transferencia exitosa muestra logs con un `traceId` común para toda la operación, y un log de la recomendación de IA que aparece **después** de que la respuesta HTTP ya fue enviada al cliente (evidencia de que la llamada a IA es asíncrona y no bloqueante).

## Métricas

Cada transferencia queda registrada en Micrometer y expuesta en Actuator:
```
curl.exe http://localhost:8080/actuator/metrics/transfers.success
curl.exe http://localhost:8080/actuator/metrics/transfers.failed
curl.exe http://localhost:8080/actuator/metrics/transfers.duration
```
`transfers.success` y `transfers.failed` cuentan transferencias completadas y fallidas (`transfers.failed` incluye un tag `reason` con el tipo de error), y `transfers.duration` mide el tiempo de cada transferencia.

## Diagnóstico de incidentes (cuellos de botella y deadlocks)

Cada bloqueo pesimista de cuenta se cronometra. Si tomar el lock de una cuenta específica tarda más de 300ms, aparece un log `WARN` señalando exactamente cuál cuenta está causando la contención:
```
WARN - Slow lock acquisition: account 5 took 850 ms
```
Además, si Postgres aborta una transacción por deadlock o el pool de conexiones agota su tiempo de espera, `GlobalExceptionHandler` lo distingue de un error genérico y responde `503 Service Unavailable` con un log claro (`Lock/timeout issue detected: ...`), en vez de mezclarse con cualquier otro error 500.

## Cómo detener todo

```
cd infra
docker compose down
```
Para además borrar los datos persistidos:
```
docker compose down -v
```
El microservicio se detiene con `Ctrl+C` en la terminal donde corre `mvnw`.

## Cómo correr el ETL

```
cd etl
pip install -r requirements.txt
python clean_transactions.py
```
Lee `raw_transactions_sample.csv` (datos con formatos de fecha/monto inconsistentes y descripciones vacías), lo limpia y normaliza, y escribe el resultado en `clean_transactions_output.csv`, imprimiendo en consola un resumen de filas procesadas y valores nulos corregidos.
