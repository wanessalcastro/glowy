package br.com.glowy.api.repository;

import br.com.glowy.api.model.Pedido;
import br.com.glowy.api.model.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Page<Pedido> findByUsuarioId(Long usuarioId, Pageable pageable);
    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);
}
