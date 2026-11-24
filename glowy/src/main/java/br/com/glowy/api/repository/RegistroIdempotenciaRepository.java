package br.com.glowy.api.repository;

import br.com.glowy.api.model.RegistroIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroIdempotenciaRepository extends JpaRepository<RegistroIdempotencia, String> {
    void deleteByPedidoId(Long pedidoId);
}
