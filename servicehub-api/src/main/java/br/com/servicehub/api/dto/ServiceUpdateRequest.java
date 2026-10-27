package br.com.servicehub.api.dto;

import br.com.servicehub.api.model.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** PUT /services/{id}: os mesmos campos do POST mais "active" (null = mantem o valor atual). */
@Schema(description = "Dados para atualizar um servico")
public record ServiceUpdateRequest(

        @NotBlank(message = "é obrigatório")
        @Size(min = 3, max = 150, message = "deve ter entre 3 e 150 caracteres")
        String title,

        @Size(max = 2000, message = "deve ter no máximo 2000 caracteres")
        String description,

        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.01", message = "deve ser maior que 0")
        @DecimalMax(value = "99999999.99", message = "deve ser no máximo 99.999.999,99")
        @Digits(integer = 8, fraction = 2, message = "deve ter no máximo 2 casas decimais")
        BigDecimal price,

        @NotNull(message = "é obrigatório")
        ServiceCategory category,

        Boolean active
) {
}
