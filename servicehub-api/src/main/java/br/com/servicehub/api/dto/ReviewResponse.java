package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long requestId,
        Long serviceId,
        Long reviewerId,
        String reviewerName,
        int rating,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getRequest().getId(), r.getRequest().getService().getId(),
                r.getReviewer().getId(), r.getReviewer().getFullName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
