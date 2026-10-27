package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.ReviewResponse;
import br.com.servicehub.api.service.ReviewService;
import br.com.servicehub.api.web.ProblemResponses;
import br.com.servicehub.api.web.PublicEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Avaliações")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PublicEndpoint
    @GetMapping("/{id}")
    @Operation(summary = "Busca uma avaliação (público)")
    @ProblemResponses({404, 500})
    public ReviewResponse get(@PathVariable Long id) {
        return reviewService.get(id);
    }
}
