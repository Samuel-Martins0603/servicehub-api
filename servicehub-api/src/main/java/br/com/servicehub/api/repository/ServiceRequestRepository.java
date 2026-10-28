package br.com.servicehub.api.repository;

import br.com.servicehub.api.model.RequestStatus;
import br.com.servicehub.api.model.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {

    @Override
    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    Page<ServiceRequest> findAll(Specification<ServiceRequest> spec, Pageable pageable);

    /** RN10: o usuario participa (como cliente ou como prestador) de alguma ordem ativa? */
    @Query("select count(o) > 0 from ServiceRequest o "
            + "where (o.client.id = :userId or o.service.provider.id = :userId) and o.status in :statuses")
    boolean existsActiveForUser(@Param("userId") Long userId, @Param("statuses") Collection<RequestStatus> statuses);

    /** RN10: o servico tem alguma ordem ativa? */
    @Query("select count(o) > 0 from ServiceRequest o where o.service.id = :serviceId and o.status in :statuses")
    boolean existsActiveForService(@Param("serviceId") Long serviceId, @Param("statuses") Collection<RequestStatus> statuses);
}
