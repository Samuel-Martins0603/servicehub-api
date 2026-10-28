package br.com.servicehub.api.repository;

import br.com.servicehub.api.model.ServiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long>, JpaSpecificationExecutor<ServiceItem> {

    @Override
    @EntityGraph(attributePaths = {"provider"})
    Page<ServiceItem> findAll(Specification<ServiceItem> spec, Pageable pageable);

    List<ServiceItem> findByProviderId(Long providerId);
}
