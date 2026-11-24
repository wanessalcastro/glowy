package br.com.glowy.api.service;

import br.com.glowy.api.dto.PagamentoRequestDTO;
import br.com.glowy.api.exception.BusinessException;
import br.com.glowy.api.exception.ResourceNotFoundException;
import br.com.glowy.api.model.Pagamento;
import br.com.glowy.api.model.Pedido;
import br.com.glowy.api.model.enums.StatusPagamento;
import br.com.glowy.api.model.enums.StatusPedido;
import br.com.glowy.api.repository.PagamentoRepository;
import br.com.glowy.api.repository.PedidoRepository;
import br.com.glowy.api.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final SecurityUtils securityUtils;

    public PagamentoService(PagamentoRepository pagamentoRepository, PedidoService pedidoService,
                            PedidoRepository pedidoRepository, SecurityUtils securityUtils) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public Pagamento registrarPagamento(PagamentoRequestDTO dto) {
        // buscarPorId já valida se o usuário é dono do pedido ou ADMIN
        Pedido pedido = pedidoService.buscarPorId(dto.pedidoId());

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new BusinessException("Apenas pedidos com status AGUARDANDO_PAGAMENTO podem ser pagos (status atual: "
                    + pedido.getStatus() + ").");
        }

        // pedido_id é único em pagamentos: se já houve um pagamento estornado, o registro é reaproveitado
        Pagamento pagamento = pagamentoRepository.findByPedidoId(pedido.getId()).orElseGet(Pagamento::new);
        if (pagamento.getStatus() == StatusPagamento.APROVADO) {
            throw new BusinessException("Este pedido já possui um pagamento aprovado.");
        }

        pagamento.setPedido(pedido);
        pagamento.setMetodo(dto.metodo());
        pagamento.setValor(pedido.getValorTotal());
        pagamento.setStatus(StatusPagamento.APROVADO);
        pagamento.setDataPagamento(LocalDateTime.now());
        pagamento.setCodigoTransacao("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        pedido.setStatus(StatusPedido.PAGO);
        pedidoRepository.save(pedido);

        return pagamentoRepository.save(pagamento);
    }

    public Page<Pagamento> listar(Pageable pageable) {
        securityUtils.validarAdmin();
        return pagamentoRepository.findAll(pageable);
    }

    public Pagamento buscarPorId(Long id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com ID: " + id));
        securityUtils.validarDonoOuAdmin(pagamento.getPedido().getUsuario().getId());
        return pagamento;
    }

    public Pagamento buscarPorPedidoId(Long pedidoId) {
        Pagamento pagamento = pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum pagamento encontrado para o pedido ID: " + pedidoId));
        securityUtils.validarDonoOuAdmin(pagamento.getPedido().getUsuario().getId());
        return pagamento;
    }

    public Page<Pagamento> buscarPorStatus(StatusPagamento status, Pageable pageable) {
        securityUtils.validarAdmin();
        return pagamentoRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Pagamento estornar(Long id) {
        Pagamento pagamento = buscarPorId(id);
        if (pagamento.getStatus() == StatusPagamento.ESTORNADO) {
            throw new BusinessException("Este pagamento já foi estornado.");
        }

        Pedido pedido = pagamento.getPedido();
        if (pedido.getStatus() == StatusPedido.ENVIADO || pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new BusinessException("Não é possível estornar o pagamento de um pedido já enviado ou entregue.");
        }

        pagamento.setStatus(StatusPagamento.ESTORNADO);
        if (pedido.getStatus() != StatusPedido.CANCELADO) {
            pedidoService.devolverEstoque(pedido);
            pedido.setStatus(StatusPedido.CANCELADO);
            pedidoRepository.save(pedido);
        }
        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public void deletar(Long id) {
        securityUtils.validarAdmin();
        Pagamento pagamento = buscarPorId(id);
        pagamentoRepository.delete(pagamento);
    }
}
