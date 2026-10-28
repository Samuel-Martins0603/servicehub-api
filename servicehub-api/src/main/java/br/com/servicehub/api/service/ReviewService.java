package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ReviewCreateRequest;
import br.com.servicehub.api.dto.ReviewResponse;
import br.com.servicehub.api.exception.ApiException;
import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.Review;
import br.com.servicehub.api.model.ServiceRequest;
import br.com.servicehub.api.repository.ReviewRepository;
import br.com.servicehub.api.repository.ServiceRequestRepository;
import br.com.servicehub.api.security.AuthUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avaliacoes. RN09: so quem abriu a ordem avalia, so depois de COMPLETED, uma unica vez. */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final ServiceRequestRepository orders;

    public ReviewService(ReviewRepository reviews, ServiceRequestRepository orders) {
        this.reviews = reviews;
        this.orders = orders;
    }

    @Transactional
    public ReviewResponse create(Long orderId, ReviewCreateRequest request, AuthUser actor) {
        ServiceRequest order = orders.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Ordem de serviço não encontrada."));
        // RN09: so o cliente que abriu a ordem avalia (403 para qualquer outra pessoa, inclusive ADMIN).
        if (!order.getClient().getId().equals(actor.id())) {
            throw ApiException.forbidden("Somente quem contratou o serviço pode avaliá-lo.");
        }
        // RN09: so depois de COMPLETED.
        if (order.getStatus() != RequestStatus.COMPLETED) {
            throw ApiException.businessRule("A ordem precisa estar COMPLETED para ser avaliada.");
        }
        // RN09: uma unica vez (a UNIQUE(request_id) do banco cobre a corrida entre duas requisicoes).
        if (reviews.existsByRequestId(orderId)) {
            throw ApiException.businessRule("Esta ordem já foi avaliada.");
        }
        Review review = new Review(order, order.getClient(), request.rating().shortValue(),
                TextSanitizer.clean(request.comment()));
        try {
            return ReviewResponse.from(reviews.saveAndFlush(review));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.businessRule("Esta ordem já foi avaliada.");
        }
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(Long id) {
        return ReviewResponse.from(reviews.findById(id)
                .orElseThrow(() -> ApiException.notFound("Avaliação não encontrada.")));
    }
}
