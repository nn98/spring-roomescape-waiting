package roomescape.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import roomescape.domain.PaymentOrder;

public class FakePaymentOrderRepository extends AbstractFakeRepository<PaymentOrder, Long>
        implements PaymentOrderRepository {

    @Override
    protected Long getId(PaymentOrder entity) {
        return entity.getId();
    }

    @Override
    protected PaymentOrder withId(PaymentOrder entity, Long id) {
        return new PaymentOrder(id, entity.orderId(), entity.amount(), entity.idempotencyKey(),
                entity.status(), entity.name(), entity.sessionId(), entity.paymentKey());
    }

    @Override
    public Optional<PaymentOrder> findByOrderId(String orderId) {
        return store.values().stream()
                .filter(order -> orderId.equals(order.orderId()))
                .findFirst();
    }

    @Override
    public List<PaymentOrder> findByNameOrderByIdDesc(String name) {
        return store.values().stream()
                .filter(order -> Objects.equals(order.name(), name))
                .sorted(Comparator.comparing(PaymentOrder::getId).reversed())
                .toList();
    }

    @Override
    public void deleteByOrderId(String orderId) {
        findByOrderId(orderId).ifPresent(order -> store.remove(order.getId()));
    }
}
