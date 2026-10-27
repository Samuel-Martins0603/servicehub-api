package br.com.servicehub.api.model;

import java.util.List;

/**
 * Maquina de estados da ordem de servico (Mapa, secao 4).
 * PENDING -> ACCEPTED -> IN_PROGRESS -> COMPLETED; PENDING/ACCEPTED -> CANCELLED.
 * COMPLETED e CANCELLED sao terminais.
 */
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /** Regra de transicao (RN08). Quem pode executar cada uma e decidido na camada de servico. */
    public boolean canTransitionTo(RequestStatus next) {
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == CANCELLED;
            case ACCEPTED -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /** Estados que impedem excluir/desativar usuario ou servico (RN10). */
    public static List<RequestStatus> activeStatuses() {
        return List.of(PENDING, ACCEPTED, IN_PROGRESS);
    }
}
