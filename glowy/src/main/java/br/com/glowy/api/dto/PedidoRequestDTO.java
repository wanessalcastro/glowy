package br.com.glowy.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PedidoRequestDTO(
        @Schema(example = "[1, 2]")
        @NotEmpty(message = "O pedido deve conter ao menos um produto")
        List<Long> produtoIds,

        @Schema(example = "Av. Paulista, 1500 - Bela Vista, São Paulo - SP")
        @NotBlank(message = "O endereço de entrega é obrigatório")
        String enderecoEntrega
) {}
