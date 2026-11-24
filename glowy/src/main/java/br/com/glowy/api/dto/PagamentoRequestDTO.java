package br.com.glowy.api.dto;

import br.com.glowy.api.model.enums.MetodoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PagamentoRequestDTO(
        @Schema(example = "1")
        @NotNull(message = "O ID do pedido é obrigatório")
        Long pedidoId,

        @Schema(example = "PIX")
        @NotNull(message = "O método de pagamento é obrigatório")
        MetodoPagamento metodo
) {}
