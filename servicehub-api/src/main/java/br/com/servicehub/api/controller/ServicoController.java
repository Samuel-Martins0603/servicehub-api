package br.com.servicehub.api.controller;

import br.com.servicehub.api.dto.ServicoRequestDTO;
import br.com.servicehub.api.dto.ServicoResponseDTO;
import br.com.servicehub.api.exception.ApiErrorResponse;
import br.com.servicehub.api.service.ServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Camada de apresentação (Controller). Responsabilidade única: receber a
 * requisição HTTP, delegar para o ServicoService e devolver a resposta com
 * o status code correto. Repare que NÃO há nenhuma regra de negócio nem
 * acesso a repositório aqui — isso seria o "erro conceitual grave" citado
 * no slide 19 da aula.
 *
 * URI no plural ("/api/servicos"), como pede o slide 13 de boas práticas
 * de design de URI.
 */
@RestController
@RequestMapping("/api/servicos")
@Tag(name = "Serviços", description = "Operações de CRUD sobre os serviços oferecidos pelo ServiceHub")
public class ServicoController {

    private final ServicoService service;

    public ServicoController(ServicoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Lista todos os serviços",
            description = "Retorna a lista completa de serviços cadastrados, ativos e inativos."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<ServicoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um serviço pelo id",
            description = "Retorna os dados completos de um único serviço a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço encontrado"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ServicoResponseDTO> buscarPorId(
            @Parameter(description = "Identificador do serviço", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(
            summary = "Cria um novo serviço",
            description = "Cadastra um novo serviço. Campos obrigatórios: nome, preço e duração em minutos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ServicoResponseDTO> criar(@Valid @RequestBody ServicoRequestDTO dto) {
        ServicoResponseDTO criado = service.criar(dto);

        // Boa prática REST (slide 11): no 201 Created, o header Location
        // aponta para a URI do recurso recém-criado.
        URI location = URI.create("/api/servicos/" + criado.id());
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um serviço existente",
            description = "Substitui integralmente os dados do serviço identificado por id (PUT = substituição completa)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ServicoResponseDTO> atualizar(
            @Parameter(description = "Identificador do serviço", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ServicoRequestDTO dto
    ) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remove um serviço",
            description = "Exclui definitivamente o serviço identificado por id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Serviço removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Serviço não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deletar(
            @Parameter(description = "Identificador do serviço", example = "1")
            @PathVariable Long id
    ) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
