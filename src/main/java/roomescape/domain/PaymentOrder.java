package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "payment_order")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", unique = true)
    private String orderId;

    private Long amount;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private PaymentOrderStatus status;

    private String name;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "payment_key")
    private String paymentKey;

    protected PaymentOrder() {
    }

    public PaymentOrder(Long id, String orderId, Long amount, String idempotencyKey,
                        PaymentOrderStatus status, String name, Long sessionId, String paymentKey) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.name = name;
        this.sessionId = sessionId;
        this.paymentKey = paymentKey;
    }

    public static PaymentOrder prepare(String orderId, Long amount) {
        return new PaymentOrder(null, orderId, amount, UUID.randomUUID().toString(),
                PaymentOrderStatus.PENDING, null, null, null);
    }

    public PaymentOrder confirmed(String name, Long sessionId, String paymentKey) {
        return new PaymentOrder(id, orderId, amount, idempotencyKey,
                PaymentOrderStatus.CONFIRMED, name, sessionId, paymentKey);
    }

    public PaymentOrder failed(String name, Long sessionId) {
        return new PaymentOrder(id, orderId, amount, idempotencyKey,
                PaymentOrderStatus.FAILED, name, sessionId, paymentKey);
    }

    public PaymentOrder unknown(String name, Long sessionId, String paymentKey) {
        return new PaymentOrder(id, orderId, amount, idempotencyKey,
                PaymentOrderStatus.UNKNOWN, name, sessionId, paymentKey);
    }

    public PaymentOrder retryable(String name, Long sessionId) {
        return new PaymentOrder(id, orderId, amount, idempotencyKey,
                PaymentOrderStatus.PENDING, name, sessionId, paymentKey);
    }

    public boolean isConfirmed() {
        return status == PaymentOrderStatus.CONFIRMED;
    }

    public Long getId() {
        return id;
    }

    public String orderId() {
        return orderId;
    }

    public Long amount() {
        return amount;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public PaymentOrderStatus status() {
        return status;
    }

    public String name() {
        return name;
    }

    public Long sessionId() {
        return sessionId;
    }

    public String paymentKey() {
        return paymentKey;
    }
}
