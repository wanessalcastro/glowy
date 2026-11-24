package br.com.glowy.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiKeyResponseDTO(
        @Schema(example = "1") Long usuarioId,
        @Schema(example = "Camila Andrade") String nome,
        @Schema(example = "glowy-9f8a7b6c5d4e3f2a") String apiKey,
        @Schema(example = "Envie esta chave no cabeçalho X-API-Key") String instrucao
) {}
