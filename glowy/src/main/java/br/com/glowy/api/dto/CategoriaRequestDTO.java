package br.com.glowy.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequestDTO(
        @Schema(example = "Boxes Mensais")
        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(min = 2, max = 60, message = "O nome deve ter entre 2 e 60 caracteres")
        String nome,

        @Schema(example = "Caixas temáticas mensais com 5 a 7 itens de maquiagem surpresa")
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255)
        String descricao
) {}
