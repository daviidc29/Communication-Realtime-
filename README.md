# UpLearn Call Service · WebRTC Signaling (Java 17 / Spring Boot)

Servicio de señalización para videollamadas 1:1 (tutor–estudiante) que expone REST + WebSocket, registra sesiones en MongoDB, publica eventos con Redis (opcional, con *fallback* local) y reporta métricas con Micrometer. Está pensado para ejecutarse en Azure/App Service.

## ✨ Características

* **Signaling WebRTC**: creación de sesiones y canal WS (`/ws/call`) para intercambiar SDP/ICE.
* **Check de elegibilidad**: integración con el Scheduler/Reservations vía `EligibilityService` (estado + pertenencia a la reserva).
* **Pub/Sub con fallback**: `RedisPubSubBridge` usa Redis si está disponible; si no, conmuta a entregas locales (cero dependencias duras).
* **Persistencia de sesiones**: `CallSession` en MongoDB (+ TTL automático con índice expirable).
* **Seguridad**: `TokenAuthFilter` + `AuthorizationService` (JWT en header o cookie). Rutas abiertas mínimas.
* **Observabilidad**: métricas de *setup time* (p95/p99), tasa de éxito y contador de fallos vía Micrometer/Prometheus.
* **CORS listo**: `SecurityConfig` habilita preflights `OPTIONS` y permite `/ws/call/**` e `/api/calls/ice-servers` sin autenticación.

## 🧱 Arquitectura 

```
React (STUN/TURN)  →  REST: POST /api/calls/session  →  CallService.create()
                     ↘ EligibilityService (Scheduler /api/reservations/{id})
WS /ws/call?token=...  ←→  RedisPubSubBridge (Redis o fallback local)
MongoDB (CallSession + TTL)
```

## 🗂️ Endpoints principales

* `POST /api/calls/session` – crea la sesión (devuelve `sessionId`, `reservationId`, `ttlSeconds`).
* `POST /api/calls/{sessionId}/end` – marca la sesión como terminada.
* `GET  /api/calls/ice-servers` – lista STUN/TURN (si hay TURN configurado).
* `GET  /api/calls/metrics` – p95/p99, successRate5m y muestras.
* `WS   /ws/call` – canal de señalización (acepta `?token=`).

Rutas públicas: `OPTIONS /**`, `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/api/calls/ice-servers`, `/ws/call/**`. El resto requiere JWT válido.

## ⚙️ Configuración (application.properties / variables de entorno)

**MongoDB**

```
spring.data.mongodb.uri=mongodb+srv://user:pass@cluster/db
```

**Redis (opcional)**
Si no defines host/puerto, el puente funciona en modo local sin romper nada.

```
spring.data.redis.host=redis.example
spring.data.redis.port=6379
```

**Scheduler/Reservations (para elegibilidad)**

```
reservations.base=https://scheduler.example/api/reservations
```

**ICE Servers**

```
stun.urls=stun:stun.l.google.com:19302,stun:stun1.l.google.com:19302
turn.urls=turns:your.turn.example:5349
turn.username=turn-user
turn.password=turn-pass
```

> `CallSession` tiene índice TTL sobre `ttl`; el factory `CallSession.create(...)` lo establece y Mongo borra documentos al expirar.

## ▶️ Ejecución local

Requisitos: **Java 17** y **Maven**.

```bash
# tests + cobertura (JaCoCo)
mvn clean verify

# ejecutar
mvn spring-boot:run
```

## 🧪 Pruebas y cobertura

* **JUnit 5 + Mockito**.
* **JaCoCo** configurado en `verify` (se genera `target/site/jacoco/jacoco.xml`).
* Cobertura mínima de ejemplo: `LINE ≥ 0.80` (ajústalo en el `jacoco-maven-plugin`).

## 📦 Despliegue

* Aplicación *stateless*. Con Redis mejoran los *fanouts* entre instancias, pero **no es obligatorio**.
* Funciona bien en **Azure App Service**:

  * Usa variables de entorno para `reservations.base`, `spring.data.mongodb.uri`, `stun/turn`.
  * Revisa CORS en el *front* y, si usas reverse proxy, agrega encabezados `X-Forwarded-*` según sea necesario.

## 🧩 Estructura relevante

```
calls/
 ├─ api/
 │   └─ CallController.java      # REST: session, end, ice-servers, metrics
 ├─ domain/
 │   └─ CallSession.java         # entidad + índices TTL/compuestos
 ├─ pubsub/
 │   └─ RedisPubSubBridge.java   # Redis → fallback local
 ├─ security/
 │   ├─ SecurityConfig.java      # reglas, CORS, rutas permitidas, 401
 │   ├─ TokenAuthFilter.java     # filtro que usa AuthorizationService
 │   └─ AuthorizationService.java# parseo de JWT (header o cookie)
 └─ service/
     ├─ EligibilityService.java  # llamada a Scheduler/reservations
     ├─ QualityMetricsService.java
     └─ CallSessionService.java  # (según tu implementación)
```

## 🧰 Ejemplos rápidos

**Crear sesión**

```bash
curl -X POST https://<host>/api/calls/session \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"reservationId":"RES-123"}'
```

**Finalizar sesión**

```bash
curl -X POST https://<host>/api/calls/ABC123/end -H "Authorization: Bearer <JWT>"
```

**ICE servers**

```bash
curl https://<host>/api/calls/ice-servers
```

