package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ServiceCreateRequest;
import br.com.servicehub.api.dto.ServiceResponse;
import br.com.servicehub.api.dto.ServiceUpdateRequest;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceCategory;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.ReviewRepository;
import br.com.servicehub.api.repository.ServiceItemRepository;
import br.com.servicehub.api.repository.ServiceRequestRepository;
import br.com.servicehub.api.repository.UserRepository;
import br.com.servicehub.api.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceItemServiceTest {

    @Mock ServiceItemRepository services;
    @Mock UserRepository users;
    @Mock ServiceRequestRepository orders;
    @Mock ReviewRepository reviews;
    @InjectMocks ServiceItemService serviceItemService;

    private final User provider = TestData.user(1L, "Maria Silva", Role.PROVIDER);
    private final User otherProvider = TestData.user(2L, "Carlos Souza", Role.PROVIDER);
    private final User client = TestData.user(3L, "Joao Pereira", Role.CLIENT);
    private final User admin = TestData.user(4L, "Admin Master", Role.ADMIN);

    private ServiceUpdateRequest updateRequest(Boolean active) {
        return new ServiceUpdateRequest("Novo título", "Nova descrição", new BigDecimal("200.00"),
                ServiceCategory.ELETRICA, active);
    }

    @Test
    @DisplayName("RN04: o prestador do serviço criado é o usuário do token")
    void rn04_providerIsTheAuthenticatedUser() {
        when(users.findById(1L)).thenReturn(Optional.of(provider));
        when(services.save(any(ServiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceResponse response = serviceItemService.create(
                new ServiceCreateRequest("Instalação elétrica", "Tomadas", new BigDecimal("150.00"), ServiceCategory.ELETRICA),
                TestData.auth(provider));

        ArgumentCaptor<ServiceItem> saved = ArgumentCaptor.forClass(ServiceItem.class);
        verify(services).save(saved.capture());
        assertThat(saved.getValue().getProvider().getId()).isEqualTo(1L);
        assertThat(response.providerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("RN04: CLIENT não publica serviço (403)")
    void rn04_clientCannotPublish() {
        TestData.assertApiError(() -> serviceItemService.create(
                        new ServiceCreateRequest("Instalação elétrica", null, new BigDecimal("150.00"), ServiceCategory.ELETRICA),
                        TestData.auth(client)),
                HttpStatus.FORBIDDEN, "forbidden");
        verify(services, never()).save(any());
    }

    @Test
    @DisplayName("RN05: outro prestador não altera o serviço (403, anti-BOLA)")
    void rn05_notTheOwnerCannotUpdate() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        when(services.findById(10L)).thenReturn(Optional.of(item));

        TestData.assertApiError(() -> serviceItemService.update(10L, updateRequest(null), TestData.auth(otherProvider)),
                HttpStatus.FORBIDDEN, "forbidden");
        verify(services, never()).save(any());
    }

    @Test
    @DisplayName("RN05: o dono altera o serviço")
    void rn05_ownerCanUpdate() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        when(services.findById(10L)).thenReturn(Optional.of(item));
        when(services.save(any(ServiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceResponse response = serviceItemService.update(10L, updateRequest(null), TestData.auth(provider));

        assertThat(response.title()).isEqualTo("Novo título");
        assertThat(response.price()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("RN05: ADMIN altera qualquer serviço")
    void rn05_adminCanUpdateAnyService() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        when(services.findById(10L)).thenReturn(Optional.of(item));
        when(services.save(any(ServiceItem.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceResponse response = serviceItemService.update(10L, updateRequest(null), TestData.auth(admin));

        assertThat(response.title()).isEqualTo("Novo título");
    }

    @Test
    @DisplayName("RN10: serviço com ordens ativas não pode ser desativado (409)")
    void rn10_cannotDeactivateServiceWithActiveOrders() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        when(services.findById(10L)).thenReturn(Optional.of(item));
        when(orders.existsActiveForService(eq(10L), anyCollection())).thenReturn(true);

        TestData.assertApiError(() -> serviceItemService.update(10L, updateRequest(false), TestData.auth(provider)),
                HttpStatus.CONFLICT, "business-rule");
        assertThat(item.isActive()).isTrue();
    }

    @Test
    @DisplayName("RN10: serviço com ordens ativas não pode ser excluído (409)")
    void rn10_cannotDeleteServiceWithActiveOrders() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        when(services.findById(10L)).thenReturn(Optional.of(item));
        when(orders.existsActiveForService(eq(10L), anyCollection())).thenReturn(true);

        TestData.assertApiError(() -> serviceItemService.delete(10L, TestData.auth(provider)),
                HttpStatus.CONFLICT, "business-rule");
        assertThat(item.isActive()).isTrue();
    }

    @Test
    @DisplayName("Exclusão é lógica: sem ordens ativas, is_active vira false (nunca DELETE físico)")
    void deleteIsLogical() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        when(services.findById(10L)).thenReturn(Optional.of(item));
        when(orders.existsActiveForService(eq(10L), anyCollection())).thenReturn(false);

        serviceItemService.delete(10L, TestData.auth(provider));

        assertThat(item.isActive()).isFalse();
        verify(services, never()).delete(any());
        verify(services, never()).deleteById(any());
    }

    @Test
    @DisplayName("RN12: serviço inativo responde 404 ao público (anônimo)")
    void rn12_inactiveServiceIs404ForThePublic() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        item.setActive(false);
        when(services.findById(10L)).thenReturn(Optional.of(item));

        TestData.assertApiError(() -> serviceItemService.get(10L, null), HttpStatus.NOT_FOUND, "not-found");
        TestData.assertApiError(() -> serviceItemService.get(10L, TestData.auth(client)), HttpStatus.NOT_FOUND, "not-found");
    }

    @Test
    @DisplayName("RN12: dono e ADMIN continuam vendo o serviço inativo")
    void rn12_ownerAndAdminStillSeeInactiveService() {
        ServiceItem item = TestData.service(10L, provider, "150.00");
        item.setActive(false);
        when(services.findById(10L)).thenReturn(Optional.of(item));

        assertThat(serviceItemService.get(10L, TestData.auth(provider)).active()).isFalse();
        assertThat(serviceItemService.get(10L, TestData.auth(admin)).active()).isFalse();
    }
}
