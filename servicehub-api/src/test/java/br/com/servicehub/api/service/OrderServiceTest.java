package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.OrderCreateRequest;
import br.com.servicehub.api.dto.OrderResponse;
import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.ServiceRequest;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.ServiceItemRepository;
import br.com.servicehub.api.repository.ServiceRequestRepository;
import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock ServiceRequestRepository orders;
    @Mock ServiceItemRepository services;
    @Mock UserRepository users;
    @InjectMocks OrderService orderService;

    private final User provider = TestData.user(1L, "Maria Silva", Role.PROVIDER);
    private final User client = TestData.user(2L, "Joao Pereira", Role.CLIENT);
    private final User stranger = TestData.user(3L, "Carlos Souza", Role.CLIENT);
    private final User admin = TestData.user(4L, "Admin Master", Role.ADMIN);

    private ServiceRequest orderIn(RequestStatus status) {
        ServiceItem service = TestData.service(10L, provider, "150.00");
        ServiceRequest order = TestData.order(31L, service, client, status);
        when(orders.findById(31L)).thenReturn(Optional.of(order));
        return order;
    }

    // ---------------- criação: RN06 e RN07 ----------------

    @Test
    @DisplayName("RN06: ADMIN não abre ordens (403)")
    void rn06_adminCannotOpenOrders() {
        TestData.assertApiError(() -> orderService.create(new OrderCreateRequest(10L, null, null), TestData.auth(admin)),
                HttpStatus.FORBIDDEN, "forbidden");
        verify(orders, never()).save(any());
    }

    @Test
    @DisplayName("RN06: ninguém contrata o próprio serviço (409)")
    void rn06_cannotHireOwnService() {
        when(services.findById(10L)).thenReturn(Optional.of(TestData.service(10L, provider, "150.00")));

        TestData.assertApiError(() -> orderService.create(new OrderCreateRequest(10L, null, null), TestData.auth(provider)),
                HttpStatus.CONFLICT, "business-rule");
        verify(orders, never()).save(any());
    }

    @Test
    @DisplayName("RN06: serviço inativo não pode ser contratado (409)")
    void rn06_inactiveServiceCannotBeHired() {
        ServiceItem service = TestData.service(10L, provider, "150.00");
        service.setActive(false);
        when(services.findById(10L)).thenReturn(Optional.of(service));

        TestData.assertApiError(() -> orderService.create(new OrderCreateRequest(10L, null, null), TestData.auth(client)),
                HttpStatus.CONFLICT, "business-rule");
    }

    @Test
    @DisplayName("RN06: serviço inexistente responde 404")
    void rn06_unknownServiceIs404() {
        when(services.findById(99L)).thenReturn(Optional.empty());

        TestData.assertApiError(() -> orderService.create(new OrderCreateRequest(99L, null, null), TestData.auth(client)),
                HttpStatus.NOT_FOUND, "not-found");
    }

    @Test
    @DisplayName("RN06: CLIENT contrata e a ordem nasce PENDING")
    void rn06_clientHiresAndOrderStartsPending() {
        when(services.findById(10L)).thenReturn(Optional.of(TestData.service(10L, provider, "150.00")));
        when(users.findById(2L)).thenReturn(Optional.of(client));
        when(orders.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.create(new OrderCreateRequest(10L, null, "Apto 302"), TestData.auth(client));

        assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
        assertThat(response.clientId()).isEqualTo(2L);
        assertThat(response.providerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("RN07: totalPrice é uma cópia do preço do serviço no momento da contratação")
    void rn07_totalPriceIsFrozenAtCreation() {
        ServiceItem service = TestData.service(10L, provider, "150.00");
        when(services.findById(10L)).thenReturn(Optional.of(service));
        when(users.findById(2L)).thenReturn(Optional.of(client));
        when(orders.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.create(new OrderCreateRequest(10L, null, null), TestData.auth(client));
        service.setPrice(new java.math.BigDecimal("999.00")); // o prestador reajusta depois

        ArgumentCaptor<ServiceRequest> saved = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().getTotalPrice()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("RN07: a entidade não tem setter de totalPrice (não muda depois)")
    void rn07_noSetterForTotalPrice() {
        assertThat(Arrays.stream(ServiceRequest.class.getMethods()).map(Method::getName))
                .doesNotContain("setTotalPrice");
    }

    // ---------------- RN08: máquina de estados ----------------

    @ParameterizedTest(name = "RN08: prestador faz {0} -> {1}")
    @CsvSource({"PENDING,ACCEPTED", "ACCEPTED,IN_PROGRESS", "IN_PROGRESS,COMPLETED",
            "PENDING,CANCELLED", "ACCEPTED,CANCELLED"})
    void rn08_providerPerformsValidTransitions(RequestStatus from, RequestStatus to) {
        ServiceRequest order = orderIn(from);
        when(orders.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.changeStatus(31L, to, TestData.auth(provider));

        assertThat(response.status()).isEqualTo(to);
        assertThat(order.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest(name = "RN08: {0} -> {1} é transição inválida (409 invalid-transition)")
    @CsvSource({"COMPLETED,PENDING", "PENDING,COMPLETED", "PENDING,IN_PROGRESS", "PENDING,PENDING",
            "ACCEPTED,COMPLETED", "ACCEPTED,PENDING", "IN_PROGRESS,CANCELLED", "IN_PROGRESS,ACCEPTED",
            "COMPLETED,CANCELLED", "CANCELLED,ACCEPTED", "CANCELLED,PENDING"})
    void rn08_invalidTransitionsAre409(RequestStatus from, RequestStatus to) {
        ServiceRequest order = orderIn(from);

        TestData.assertApiError(() -> orderService.changeStatus(31L, to, TestData.auth(provider)),
                HttpStatus.CONFLICT, "invalid-transition");
        assertThat(order.getStatus()).isEqualTo(from);
    }

    @Test
    @DisplayName("RN08: a mensagem da transição inválida explica a recusa")
    void rn08_invalidTransitionMessageExplainsTheRefusal() {
        orderIn(RequestStatus.COMPLETED);

        Throwable thrown = catchThrowable(
                () -> orderService.changeStatus(31L, RequestStatus.PENDING, TestData.auth(provider)));

        assertThat(thrown).isInstanceOf(ApiException.class)
                .hasMessage("Não é possível mudar de COMPLETED para PENDING.");
    }

    @ParameterizedTest(name = "RN08: o cliente não pode mudar para {0} (403)")
    @CsvSource({"ACCEPTED,PENDING", "IN_PROGRESS,ACCEPTED", "COMPLETED,IN_PROGRESS"})
    void rn08_clientCannotAcceptStartOrComplete(RequestStatus to, RequestStatus from) {
        orderIn(from);

        TestData.assertApiError(() -> orderService.changeStatus(31L, to, TestData.auth(client)),
                HttpStatus.FORBIDDEN, "forbidden");
    }

    @Test
    @DisplayName("RN08: o cliente que abriu a ordem pode cancelá-la")
    void rn08_clientCanCancelPendingOrder() {
        ServiceRequest order = orderIn(RequestStatus.PENDING);
        when(orders.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.changeStatus(31L, RequestStatus.CANCELLED, TestData.auth(client));

        assertThat(order.getStatus()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    @DisplayName("RN08: quem não tem relação com a ordem recebe 403 ANTES de a transição ser avaliada")
    void rn08_strangerGets403BeforeTransitionIsEvaluated() {
        orderIn(RequestStatus.COMPLETED);

        // COMPLETED -> PENDING seria 409, mas o desconhecido já recebe 403
        TestData.assertApiError(() -> orderService.changeStatus(31L, RequestStatus.PENDING, TestData.auth(stranger)),
                HttpStatus.FORBIDDEN, "forbidden");
    }

    @Test
    @DisplayName("RN08: ADMIN executa qualquer transição válida")
    void rn08_adminCanPerformAnyValidTransition() {
        ServiceRequest order = orderIn(RequestStatus.PENDING);
        when(orders.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.changeStatus(31L, RequestStatus.ACCEPTED, TestData.auth(admin));

        assertThat(order.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
    }

    @Test
    @DisplayName("RN08: ordem inexistente responde 404")
    void rn08_unknownOrderIs404() {
        when(orders.findById(99L)).thenReturn(Optional.empty());

        TestData.assertApiError(() -> orderService.changeStatus(99L, RequestStatus.ACCEPTED, TestData.auth(provider)),
                HttpStatus.NOT_FOUND, "not-found");
    }

    // ---------------- RN13: visibilidade ----------------

    @Test
    @DisplayName("RN13: quem não participa da ordem recebe 403 ao consultá-la")
    void rn13_strangerCannotReadOrder() {
        orderIn(RequestStatus.PENDING);

        TestData.assertApiError(() -> orderService.get(31L, TestData.auth(stranger)), HttpStatus.FORBIDDEN, "forbidden");
    }

    @Test
    @DisplayName("RN13: cliente, prestador do serviço e ADMIN consultam a ordem")
    void rn13_participantsAndAdminCanReadOrder() {
        orderIn(RequestStatus.PENDING);

        assertThat(orderService.get(31L, TestData.auth(client)).id()).isEqualTo(31L);
        assertThat(orderService.get(31L, TestData.auth(provider)).id()).isEqualTo(31L);
        assertThat(orderService.get(31L, TestData.auth(admin)).id()).isEqualTo(31L);
    }
}
