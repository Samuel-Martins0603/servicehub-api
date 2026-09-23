package br.com.servicehub.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Entidade JPA: representa a tabela "servicos" no banco.
 *
 * @Entity diz ao Hibernate "esta classe vira uma tabela".
 * @Table define explicitamente o nome da tabela (senão o Hibernate usaria
 * o nome da classe).
 *
 * Repare que esta classe NÃO tem nenhuma anotação do Swagger/SpringDoc.
 * Isso é proposital: a entidade é o modelo interno de persistência; quem
 * conversa com o "mundo de fora" (e por isso precisa de @Schema, exemplos
 * etc.) são os DTOs, em br.com.servicehub.api.dto. Essa separação evita
 * expor detalhes do banco (como o ddl-auto) diretamente no contrato da API.
 */
@Entity
@Table(name = "servicos")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(nullable = false)
    private Boolean ativo = true;

    protected Servico() {
        // Construtor vazio exigido pelo JPA/Hibernate (ele instancia via reflection)
    }

    public Servico(String nome, String descricao, BigDecimal preco, Integer duracaoMinutos, Boolean ativo) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.duracaoMinutos = duracaoMinutos;
        this.ativo = ativo != null ? ativo : true;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
