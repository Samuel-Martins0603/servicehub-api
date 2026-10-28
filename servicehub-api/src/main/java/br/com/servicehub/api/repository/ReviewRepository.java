package br.com.servicehub.api.repository;

import br.com.servicehub.api.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByRequestId(Long requestId);

    @EntityGraph(attributePaths = {"reviewer", "request", "request.service"})
    Page<Review> findByRequestServiceId(Long serviceId, Pageable pageable);
}
