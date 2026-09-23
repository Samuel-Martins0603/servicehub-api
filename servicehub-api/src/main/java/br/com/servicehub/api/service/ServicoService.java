package br.com.servicehub.api.service;

import br.com.servicehub.api.dto.ServicoRequestDTO;
import br.com.servicehub.api.dto.ServicoResponseDTO;
import br.com.servicehub.api.exception.ResourceNotFoundException;
import br.com.servicehub.api.model.Servico;
import br.com.servicehub.api.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Camada de negócio (Service). É AQUI, e só aqui, que deveria crescer
 * qualquer regra mais complexa (ex.: "não permitir dois serviços com o
 * mesmo nome", "recalcular preço com desconto" etc.) — o Controller só
 * delega para cá, e o Repository só sabe conversar com o banco.
 *
 * @Service marca a classe como um bean gerenciado pelo Spring, para que
 * ela possa ser injetada no Controller via construtor (veja
 * ServicoController).
 */
@Service
public class ServicoService {

    private final ServicoRepository repository;

    // Injeção de dependência via construtor: o Spring, ao criar o bean
    // ServicoService, enxerga que ele precisa de um ServicoRepository e
    // injeta automaticamente a implementação gerada pelo Spring Data JPA.
    public ServicoService(ServicoRepository repository) {
        this.repository = repository;
    }

    public List<ServicoResponseDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(ServicoResponseDTO::fromEntity)
                .toList();
    }

    public ServicoResponseDTO buscarPorId(Long id) {
        Servico servico = buscarEntidadeOuLancar(id);
        return ServicoResponseDTO.fromEntity(servico);
    }

    public ServicoResponseDTO criar(ServicoRequestDTO dto) {
        Servico servico = new Servico(
                dto.nome(),
                dto.descricao(),
                dto.preco(),
                dto.duracaoMinutos(),
                dto.ativo()
        );
        Servico salvo = repository.save(servico);
        return ServicoResponseDTO.fromEntity(salvo);
    }

    public ServicoResponseDTO atualizar(Long id, ServicoRequestDTO dto) {
        Servico servico = buscarEntidadeOuLancar(id);

        // PUT substitui integralmente o recurso (conforme o slide 5 pede)
        servico.setNome(dto.nome());
        servico.setDescricao(dto.descricao());
        servico.setPreco(dto.preco());
        servico.setDuracaoMinutos(dto.duracaoMinutos());
        servico.setAtivo(dto.ativo() != null ? dto.ativo() : servico.getAtivo());

        Servico atualizado = repository.save(servico);
        return ServicoResponseDTO.fromEntity(atualizado);
    }

    public void deletar(Long id) {
        Servico servico = buscarEntidadeOuLancar(id);
        repository.delete(servico);
    }

    /** Método privado auxiliar: evita repetir "orElseThrow" em cada método público. */
    private Servico buscarEntidadeOuLancar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Serviço com id " + id + " não encontrado"));
    }
}
