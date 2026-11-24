package br.com.glowy.api.repository;

import br.com.glowy.api.model.Pagamento;
import br.com.glowy.api.model.enums.StatusPagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Optional<Pagamento> findByPedidoId(Long pedidoId);
    Page<Pagamento> findByStatus(StatusPagamento status, Pageable pageable);
}
