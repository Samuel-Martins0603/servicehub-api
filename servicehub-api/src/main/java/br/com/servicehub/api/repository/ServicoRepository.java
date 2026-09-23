package br.com.servicehub.api.repository;

import br.com.servicehub.api.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Camada de dados (acesso ao banco). Repare que é só uma interface — sem
 * nenhuma implementação escrita por nós.
 *
 * Ao estender JpaRepository<Servico, Long>, o Spring Data JPA gera em
 * tempo de execução uma implementação com métodos como:
 *   save(Servico)        -> INSERT ou UPDATE
 *   findById(Long)        -> SELECT ... WHERE id = ?
 *   findAll()             -> SELECT * FROM servicos
 *   deleteById(Long)      -> DELETE ... WHERE id = ?
 *   existsById(Long)
 *
 * "Servico" é o tipo da entidade; "Long" é o tipo da chave primária (id).
 *
 * @Repository não é estritamente necessária aqui (o Spring já reconhece
 * subinterfaces de JpaRepository automaticamente), mas deixamos explícita
 * para reforçar a que camada esta classe pertence.
 */
@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
}
