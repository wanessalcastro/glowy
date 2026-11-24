package br.com.glowy.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponseDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<String> detalhes
) {}
