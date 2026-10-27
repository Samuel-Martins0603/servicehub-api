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

/** POST /services. Sem providerId: o prestador e o usuario do token (RN04). */
@Schema(description = "Dados para publicar um servico")
public record ServiceCreateRequest(

        @Schema(example = "Instalação elétrica residencial")
        @NotBlank(message = "é obrigatório")
        @Size(min = 3, max = 150, message = "deve ter entre 3 e 150 caracteres")
        String title,

        @Schema(example = "Tomadas, disjuntores e fiação")
        @Size(max = 2000, message = "deve ter no máximo 2000 caracteres")
        String description,

        @Schema(example = "150.00")
        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.01", message = "deve ser maior que 0")
        @DecimalMax(value = "99999999.99", message = "deve ser no máximo 99.999.999,99")
        @Digits(integer = 8, fraction = 2, message = "deve ter no máximo 2 casas decimais")
        BigDecimal price,

        @Schema(example = "ELETRICA")
        @NotNull(message = "é obrigatório")
        ServiceCategory category
) {
}
