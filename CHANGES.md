# 변경 명세 — 통합 완결 브랜치 (`complete`)

## 개요

`jpa` 브랜치(JPA 영속성)와 `api` 브랜치(결제·타임아웃·멱등키·Rate Limit, JDBC)를 하나로 통합한 브랜치다.
두 브랜치는 베이스에서 갈라진 독립 코드베이스라 단순 `git merge`가 불가능하므로,
**`jpa`를 베이스로 `api`의 기능 계층을 얹는 가산(additive) 통합**을 했다.

| 영역 | 결정 |
|---|---|
| 영속성 | **JPA**(@Entity + `JpaRepository`). `jpa`의 검증된 영속 계층·`AbstractFakeRepository` Fake 전략 재사용 |
| 결제/레이트리밋 | 영속성과 무관한 파일은 `api`에서 그대로 이식(payment·gateway·ratelimit·인터셉터·설정) |
| JPA화 | `PaymentOrder`를 `@Entity`로, `PaymentOrderRepository`를 `JpaRepository`로 신규 작성. `Reservation`에 `amount`·`paymentKey` 추가 |
| 스키마 | `schema.sql`에 `reservation.amount/payment_key`·`payment_order` 테이블 추가(엔티티 매핑과 정합) |

---

## 1. 영속성 — JPA 결제 모델

| 파일 | 설명 |
|---|---|
| `domain/PaymentOrder.java` | `@Entity` 주문 원장. PK는 `id`(Long), `order_id`는 unique. 상태전이(`confirmed/failed/unknown/retryable`)는 새 인스턴스 반환 |
| `repository/PaymentOrderRepository.java` | `JpaRepository<PaymentOrder, Long>` + `findByOrderId`·`findByNameOrderByIdDesc`·`deleteByOrderId` 파생 쿼리 |
| `service/PaymentOrderService.java` | prepare/cancel/confirm + 실패 기록(`recordUnknown/Failed/Retryable`, `REQUIRES_NEW`로 롤백돼도 보존) |
| `domain/Reservation.java` | 엔티티에 `amount`·`paymentKey` 추가, 결제용 `transientOf` 오버로드 |

## 2. 결제·타임아웃·Rate Limit (api에서 이식, 영속성 무관)

- `payment/**`: `PaymentConfirmation/PaymentResult/PaymentStatus`, `gateway/PaymentGateway`, `gateway/toss/*`(타임아웃 예외 변환·Idempotency-Key 헤더), 결제·레이트리밋 예외 5종.
- `ratelimit/**`: `TokenBucketRateLimiter`, 인바운드/아웃바운드/재시도 인터셉터.
- `config/TossClientConfig`(타임아웃·재시도·아웃바운드 레이트리밋 RestClient), `config/RateLimitConfig`(`MappedInterceptor` 인바운드).
- `controller/PaymentController`(`/payments/prepare`·`/payments?userName=`), 결제 DTO 3종.
- `exception/ProblemDetailsAdvice`: 결제·레이트리밋 핸들러 병합(429/503/504 + Retry-After).

## 3. 공통 계층 재조정 (jpa ← api의 결제 반영)

- `service/SessionService`: `makeReservation(PaymentReservationRequest)`에 결제 흐름(prepare→금액검증→`gateway.confirm`→예외별 기록→`CONFIRMED`+예약저장) 그래프팅. jpa의 세션/대기 승급 로직(`session.promoteCandidate`)·JpaRepository 시그니처 유지.
- `service/ReservationService.save(name, session, amount, paymentKey)` 오버로드.
- `controller/ReservationController` POST → `PaymentReservationRequest`.
- `Booking`·`BookingResponse`·`ReservationResponse`에 `amount` 추가.

## 4. 빌드·설정

- `build.gradle`: `jpa` 기반 + `mockwebserver`(test) 추가.
- `application.properties`(main/test): JPA 설정 + `toss.*`·`rate-limit.*`·`outbound-rate-limit.*`. 테스트는 `spring.sql.init.mode=never`로 SQL 초기화를 끄고 Hibernate가 깨끗한 테이블을 생성(`@DataJpaTest` 시드 충돌 방지).

## 5. 테스트

- `AbstractFakeRepository` 기반 `FakePaymentOrderRepository` 신규.
- `SessionServiceTest`: 결제 케이스(금액불일치) + **read timeout→UNKNOWN 기록 경로**(보강) 추가.
- 결제/레이트리밋/학습 테스트 이식(`TossPaymentGatewayTest`, `ratelimit/*`, `learning/*`, `RateLimitIntegrationTest`).
- 컨트롤러/통합 테스트를 결제 흐름(`PaymentReservationRequest`, `@MockitoBean PaymentGateway`, `payment_order` 시드)으로 정비.
- 전체 141개 그린, `bootRun` 스모크(JPA 영속·결제 prepare·인바운드 429) 확인.

---

### TODO (api에서 승계)

- 자동 승격 예약 결제 처리(승급 예약은 `amount 0`).
- "확인 필요(UNKNOWN)" 주문 수렴: 결제 조회 API + 멱등 재시도.
