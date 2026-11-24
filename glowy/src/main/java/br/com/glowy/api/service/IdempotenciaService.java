package br.com.glowy.api.service;

import br.com.glowy.api.dto.PedidoRequestDTO;
import br.com.glowy.api.exception.BusinessException;
import br.com.glowy.api.model.RegistroIdempotencia;
import br.com.glowy.api.repository.RegistroIdempotenciaRepository;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotenciaService {

    private final RegistroIdempotenciaRepository repository;

    public IdempotenciaService(RegistroIdempotenciaRepository repository) {
        this.repository = repository;
    }

    public String gerarChaveComposta(Long usuarioId, String idempotencyKey) {
        return usuarioId + ":" + idempotencyKey.trim();
    }

    public String calcularHashPayload(PedidoRequestDTO dto) {
        String raw = dto.produtoIds().toString() + "|" + dto.enderecoEntrega().trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }

    public Optional<RegistroIdempotencia> verificarReplay(String chaveComposta, String payloadHashAtual) {
        Optional<RegistroIdempotencia> existente = repository.findById(chaveComposta);
        if (existente.isPresent()) {
            RegistroIdempotencia reg = existente.get();
            if (!reg.getPayloadHash().equals(payloadHashAtual)) {
                throw new BusinessException("A chave X-Idempotency-Key já foi utilizada com um corpo de requisição diferente.");
            }
            return Optional.of(reg);
        }
        return Optional.empty();
    }

    public void registrar(String chaveComposta, String payloadHash, Long pedidoId, int statusHttp) {
        repository.saveAndFlush(new RegistroIdempotencia(chaveComposta, payloadHash, pedidoId, statusHttp));
    }

    /** Remove os registros de idempotência ligados a um pedido excluído (evita replay apontando para 404). */
    public void removerPorPedido(Long pedidoId) {
        repository.deleteByPedidoId(pedidoId);
    }
}
