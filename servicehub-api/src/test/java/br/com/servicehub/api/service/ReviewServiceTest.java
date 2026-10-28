package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ReviewCreateRequest;
import br.com.servicehub.api.dto.ReviewResponse;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.Review;
import br.com.servicehub.api.model.Role;
import br.com.servicehub.api.model.ServiceItem;
import br.com.servicehub.api.model.ServiceRequest;
import br.com.servicehub.api.model.User;
import br.com.servicehub.api.repository.ReviewRepository;
import br.com.servicehub.api.repository.ServiceRequestRepository;
import br.com.servicehub.api.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock ReviewRepository reviews;
    @Mock ServiceRequestRepository orders;
    @InjectMocks ReviewService reviewService;

    private final User provider = TestData.user(1L, "Maria Silva", Role.PROVIDER);
    private final User client = TestData.user(2L, "Joao Pereira", Role.CLIENT);
    private final User stranger = TestData.user(3L, "Carlos Souza", Role.CLIENT);
    private final User admin = TestData.user(4L, "Admin Master", Role.ADMIN);

    private void givenOrder(RequestStatus status) {
        ServiceItem service = TestData.service(10L, provider, "150.00");
        ServiceRequest order = TestData.order(31L, service, client, status);
        when(orders.findById(31L)).thenReturn(Optional.of(order));
    }

    @Test
    @DisplayName("RN09: o cliente da ordem concluída avalia com nota e comentário")
    void rn09_clientReviewsCompletedOrder() {
        givenOrder(RequestStatus.COMPLETED);
        when(reviews.existsByRequestId(31L)).thenReturn(false);
        when(reviews.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.create(31L, new ReviewCreateRequest(5, "Pontual e caprichosa."),
                TestData.auth(client));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.reviewerId()).isEqualTo(2L);
        assertThat(response.requestId()).isEqualTo(31L);
    }

    @Test
    @DisplayName("RN09: quem não abriu a ordem não avalia (403), nem ADMIN")
    void rn09_onlyTheClientWhoOpenedTheOrderReviews() {
        givenOrder(RequestStatus.COMPLETED);

        TestData.assertApiError(() -> reviewService.create(31L, new ReviewCreateRequest(5, null), TestData.auth(stranger)),
                HttpStatus.FORBIDDEN, "forbidden");
        TestData.assertApiError(() -> reviewService.create(31L, new ReviewCreateRequest(5, null), TestData.auth(admin)),
                HttpStatus.FORBIDDEN, "forbidden");
        verify(reviews, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RN09: só depois de COMPLETED (409 se a ordem não foi concluída)")
    void rn09_onlyAfterCompleted() {
        givenOrder(RequestStatus.IN_PROGRESS);

        TestData.assertApiError(() -> reviewService.create(31L, new ReviewCreateRequest(4, null), TestData.auth(client)),
                HttpStatus.CONFLICT, "business-rule");
        verify(reviews, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RN09: uma única vez (409 se a ordem já foi avaliada)")
    void rn09_onlyOnce() {
        givenOrder(RequestStatus.COMPLETED);
        when(reviews.existsByRequestId(31L)).thenReturn(true);

        TestData.assertApiError(() -> reviewService.create(31L, new ReviewCreateRequest(4, null), TestData.auth(client)),
                HttpStatus.CONFLICT, "business-rule");
        verify(reviews, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RN09: ordem inexistente responde 404")
    void rn09_unknownOrderIs404() {
        when(orders.findById(99L)).thenReturn(Optional.empty());

        TestData.assertApiError(() -> reviewService.create(99L, new ReviewCreateRequest(4, null), TestData.auth(client)),
                HttpStatus.NOT_FOUND, "not-found");
    }

    @Test
    @DisplayName("Sanitização (Aula 8): o comentário passa por escape de HTML antes de gravar")
    void commentIsHtmlEscapedBeforeSaving() {
        givenOrder(RequestStatus.COMPLETED);
        when(reviews.existsByRequestId(31L)).thenReturn(false);
        when(reviews.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.create(31L, new ReviewCreateRequest(5, "<script>alert(1)</script>"), TestData.auth(client));

        ArgumentCaptor<Review> saved = ArgumentCaptor.forClass(Review.class);
        verify(reviews).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getComment()).doesNotContain("<").doesNotContain(">")
                .isEqualTo("&lt;script&gt;alert(1)&lt;/script&gt;");
    }
}
