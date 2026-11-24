package br.com.glowy.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProdutoRequestDTO(
        @Schema(example = "Glowy Box Edição Velvet Glam")
        @NotBlank(message = "O nome do produto é obrigatório")
        String nome,

        @Schema(example = "Box com paleta de sombras, lip tint, blush cremoso e máscara de cílios")
        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @Schema(example = "119.90")
        @NotNull(message = "O preço é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal preco,

        @Schema(example = "45")
        @NotNull(message = "O estoque é obrigatório")
        @Min(value = 0, message = "O estoque não pode ser negativo")
        Integer estoque,

        @Schema(example = "true")
        boolean edicaoAssinatura,

        @Schema(example = "1")
        @NotNull(message = "O ID da categoria é obrigatório")
        Long categoriaId
) {}
