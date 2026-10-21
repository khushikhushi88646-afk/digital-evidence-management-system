# Digital Evidence Management System with Redis-Based Evidence Cache
**Team Avengers · GEHU Dehradun** — Java (Spring Boot) + HTML/CSS/JS (Three.js 3D) + MySQL/H2 + Redis

## Kya-kya hai
| Feature | Kaise bana hai |
|---|---|
| Login + roles (ADMIN / INVESTIGATOR) | Spring Security + JWT. Session Redis mein store hoti hai, isliye **logout sach mein token kill karta hai** |
| Evidence upload + SHA-256 | File server pe save hoti hai, hash server calculate karta hai |
| Integrity verify | File ka hash dobara nikal ke stored hash se compare → VERIFIED / TAMPERED |
| Redis cache | Evidence metadata, case-wise list, search results, cases list. TTL 10 min, upload/verify/delete pe invalidate |
| Chain of custody | Har action (LOGIN, UPLOAD, VIEW, VERIFY, DOWNLOAD, DELETE) ka log. Har entry ka hash = SHA-256(pichli entry ka hash + apna data) → DB mein koi row edit kare to **Verify chain** toot jaati hai |
| Cache benchmark | Same query N baar DB se vs Redis se, speed-up dikhata hai (proposal deliverable #5) |
| 3D UI | Login pe 3D "evidence vault"; dashboard pe live 3D request-flow (App → Redis → DB): HIT = chhota raasta, MISS = DB tak |

## Run kaise karein
**Option A – Docker (sabse easy)**
```
docker compose up --build
```
Browser: http://localhost:8080

**Option B – locally (Java 17+, Maven, Redis chahiye)**
```
redis-server                      # ek terminal mein (ya: docker run -p 6379:6379 redis:7)
mvn spring-boot:run               # default: H2 file database, kuch install nahi karna
# MySQL ke saath:
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
**Login:** `admin / admin123` (delete + user create allowed) · `investigator / invest123`

## Demo script (viva / placement interview)
1. Login → 3D page dikhao, admin se sign in.
2. Cases → naya case banao. Evidence → file upload karo, SHA-256 dikhao.
3. **View** dabao → badge `DATABASE`; dobara dabao → `REDIS` (ms mein difference), Overview pe 3D flow animate hota hai.
4. File ko `uploads/` folder mein manually edit karo → **Verify** → `TAMPERED`.
5. Chain of custody → **Verify chain** → VALID. (H2/MySQL mein ek audit row ka `details` badlo → BROKEN.)
6. Cache benchmark chalao → DB vs Redis.

## API (Postman ke liye)
`POST /api/auth/login` · `POST /api/auth/logout` · `POST /api/auth/register` (admin)
`GET/POST /api/cases` · `PUT /api/cases/{id}/status`
`POST /api/evidence` (multipart: file, caseId, title, type, description) · `GET /api/evidence?caseId=|q=` · `GET /api/evidence/{id}` · `POST /api/evidence/{id}/verify` · `GET /api/evidence/{id}/download` · `DELETE /api/evidence/{id}` (admin)
`GET /api/audit` · `GET /api/audit/verify-chain` (admin) · `GET /api/stats/overview` · `GET /api/stats/benchmark?caseId=1&n=1000`
Header: `Authorization: Bearer <token>`

## Structure
```
src/main/java/com/avengers/evidence/
  config/      SecurityConfig, DataSeeder
  security/    JwtService, JwtFilter (Redis session check)
  model/       AppUser, CaseFile, Evidence, AuditLog
  repo/        Spring Data JPA repositories
  service/     CacheService (Redis), EvidenceService, AuditService (hash chain), StatsService
  controller/  Auth, Case, Evidence, Audit, Stats
src/main/resources/static/   login.html, dashboard.html, css/, js/ (Three.js bundled locally)
```

## Interview mein bolne wale points
- Cache-aside pattern: pehle Redis, miss pe DB, phir Redis mein put; write pe invalidate.
- Hash-chained audit log (blockchain jaisa idea) — tamper-evidence, sirf "log" nahi.
- JWT stateless hota hai, isliye Redis mein session id rakhi taaki logout/revoke possible ho.
- Path-traversal se bachne ke liye stored filename UUID + `normalize().startsWith(dir)` check.

## Limitations (honest)
Demo-level system hai (proposal ke assumptions ke mutabiq). Production ke liye: JWT secret env var se, HTTPS, file-type scanning, rate-limiting, aur unit tests add karna chahiye.
