package br.com.servicehub.api.support;

import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceCategory;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.ServiceRequest;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.security.AuthUser;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** Fabricas de entidades para testes unitarios (ids definidos por reflexao) e asserts de ApiException. */
public final class TestData {

    private TestData() {
    }

    public static User user(Long id, String name, Role role) {
        User u = new User(name, name.toLowerCase().replace(' ', '.') + "@test.com", "hash", role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    public static ServiceItem service(Long id, User provider, String price) {
        ServiceItem s = new ServiceItem("Instalação elétrica", "Tomadas e fiação", new BigDecimal(price),
                ServiceCategory.ELETRICA, provider);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    public static ServiceRequest order(Long id, ServiceItem service, User client, RequestStatus status) {
        ServiceRequest o = new ServiceRequest(service, client, null, null);
        ReflectionTestUtils.setField(o, "id", id);
        o.setStatus(status);
        return o;
    }

    public static AuthUser auth(User u) {
        return new AuthUser(u.getId(), u.getEmail(), u.getRole());
    }

    public static void assertApiError(ThrowingCallable call, HttpStatus status, String typeSuffix) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(ApiException.class);
        ApiException ex = (ApiException) thrown;
        assertThat(ex.getStatus()).isEqualTo(status);
        assertThat(ex.getTypeSuffix()).isEqualTo(typeSuffix);
    }
}
