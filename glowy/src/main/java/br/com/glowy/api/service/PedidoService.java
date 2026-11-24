package br.com.glowy.api.service;

import br.com.glowy.api.dto.PedidoRequestDTO;
import br.com.glowy.api.exception.BusinessException;
import br.com.glowy.api.exception.ResourceNotFoundException;
import br.com.glowy.api.model.Pedido;
import br.com.glowy.api.model.Produto;
import br.com.glowy.api.model.Usuario;
import br.com.glowy.api.model.enums.StatusPagamento;
import br.com.glowy.api.model.enums.StatusPedido;
import br.com.glowy.api.repository.PedidoRepository;
import br.com.glowy.api.repository.ProdutoRepository;
import br.com.glowy.api.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoService produtoService;
    private final ProdutoRepository produtoRepository;
    private final IdempotenciaService idempotenciaService;
    private final SecurityUtils securityUtils;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoService produtoService,
                         ProdutoRepository produtoRepository, IdempotenciaService idempotenciaService,
                         SecurityUtils securityUtils) {
        this.pedidoRepository = pedidoRepository;
        this.produtoService = produtoService;
        this.produtoRepository = produtoRepository;
        this.idempotenciaService = idempotenciaService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public Pedido criar(PedidoRequestDTO dto, String chaveComposta, String payloadHash) {
        Set<Long> idsUnicos = new HashSet<>(dto.produtoIds());
        if (idsUnicos.size() != dto.produtoIds().size()) {
            throw new BusinessException("Não envie IDs de produtos duplicados no mesmo pedido.");
        }

        // O comprador é sempre o usuário autenticado pela X-API-Key
        Usuario usuario = securityUtils.getUsuarioAutenticado();
        List<Produto> produtos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Long prodId : dto.produtoIds()) {
            Produto produto = produtoService.buscarPorId(prodId);
            if (produto.getEstoque() <= 0) {
                throw new BusinessException("Produto sem estoque disponível: " + produto.getNome());
            }
            produto.setEstoque(produto.getEstoque() - 1);
            produtoRepository.save(produto);
            produtos.add(produto);
            total = total.add(produto.getPreco());
        }

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setProdutos(produtos);
        pedido.setEnderecoEntrega(dto.enderecoEntrega());
        pedido.setValorTotal(total);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        Pedido salvo = pedidoRepository.save(pedido);

        // Registra a chave de idempotência na mesma transação atômica do pedido
        idempotenciaService.registrar(chaveComposta, payloadHash, salvo.getId(), HttpStatus.CREATED.value());

        return salvo;
    }

    public Page<Pedido> listar(Pageable pageable) {
        securityUtils.validarAdmin();
        return pedidoRepository.findAll(pageable);
    }

    public Pedido buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        securityUtils.validarDonoOuAdmin(pedido.getUsuario().getId());
        return pedido;
    }

    public Page<Pedido> buscarPorUsuario(Long usuarioId, Pageable pageable) {
        securityUtils.validarDonoOuAdmin(usuarioId);
        return pedidoRepository.findByUsuarioId(usuarioId, pageable);
    }

    public Page<Pedido> buscarPorStatus(StatusPedido status, Pageable pageable) {
        securityUtils.validarAdmin();
        return pedidoRepository.findByStatus(status, pageable);
    }

    /**
     * Regras de transição:
     *  - PAGO só é definido pelo PagamentoService (POST /api/pagamentos);
     *  - CANCELADO só volta para AGUARDANDO_PAGAMENTO (e rebaixa o estoque);
     *  - AGUARDANDO_PAGAMENTO só é alcançável a partir de CANCELADO;
     *  - EM_PREPARACAO / ENVIADO / ENTREGUE exigem pagamento aprovado (apenas ADMIN);
     *  - cancelar: dono ou ADMIN, nunca após envio nem com pagamento aprovado (usar estorno).
     */
    @Transactional
    public Pedido atualizarStatus(Long id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);
        StatusPedido statusAtual = pedido.getStatus();

        if (statusAtual == novoStatus) {
            return pedido;
        }

        if (novoStatus == StatusPedido.PAGO) {
            throw new BusinessException("O status PAGO é definido apenas ao registrar o pagamento (POST /api/pagamentos).");
        }

        if (novoStatus == StatusPedido.CANCELADO) {
            if (statusAtual == StatusPedido.ENVIADO || statusAtual == StatusPedido.ENTREGUE) {
                throw new BusinessException("Não é possível cancelar um pedido já enviado ou entregue.");
            }
            if (possuiPagamentoAprovado(pedido)) {
                throw new BusinessException("Não é possível cancelar diretamente um pedido pago. Estorne o pagamento primeiro em PUT /api/pagamentos/{id}/estornar.");
            }
            devolverEstoque(pedido);
        } else {
            securityUtils.validarAdmin();

            if (statusAtual == StatusPedido.CANCELADO && novoStatus != StatusPedido.AGUARDANDO_PAGAMENTO) {
                throw new BusinessException("Um pedido cancelado só pode voltar para AGUARDANDO_PAGAMENTO.");
            }
            if (novoStatus == StatusPedido.AGUARDANDO_PAGAMENTO && statusAtual != StatusPedido.CANCELADO) {
                throw new BusinessException("O status AGUARDANDO_PAGAMENTO só pode ser restaurado a partir de CANCELADO.");
            }
            boolean avancoLogistico = novoStatus == StatusPedido.EM_PREPARACAO
                    || novoStatus == StatusPedido.ENVIADO
                    || novoStatus == StatusPedido.ENTREGUE;
            if (avancoLogistico && !possuiPagamentoAprovado(pedido)) {
                throw new BusinessException("O pedido precisa estar pago antes de avançar na logística.");
            }

            if (statusAtual == StatusPedido.CANCELADO) {
                baixarEstoque(pedido);
            }
        }

        pedido.setStatus(novoStatus);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void deletar(Long id) {
        Pedido pedido = buscarPorId(id);
        if (pedido.getStatus() == StatusPedido.ENVIADO || pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new BusinessException("Não é possível excluir um pedido já enviado ou entregue.");
        }
        if (possuiPagamentoAprovado(pedido)) {
            throw new BusinessException("Não é possível excluir um pedido com pagamento aprovado. Estorne o pagamento primeiro.");
        }
        if (pedido.getStatus() != StatusPedido.CANCELADO) {
            devolverEstoque(pedido);
        }
        // Remove também a chave de idempotência, para o replay não apontar para um pedido inexistente
        idempotenciaService.removerPorPedido(pedido.getId());
        pedidoRepository.delete(pedido);
    }

    private boolean possuiPagamentoAprovado(Pedido pedido) {
        return pedido.getStatus() == StatusPedido.PAGO
                || (pedido.getPagamento() != null && pedido.getPagamento().getStatus() == StatusPagamento.APROVADO);
    }

    public void baixarEstoque(Pedido pedido) {
        for (Produto produto : pedido.getProdutos()) {
            Produto atualizado = produtoService.buscarPorId(produto.getId());
            if (atualizado.getEstoque() <= 0) {
                throw new BusinessException("Sem estoque suficiente para reativar o produto: " + atualizado.getNome());
            }
            atualizado.setEstoque(atualizado.getEstoque() - 1);
            produtoRepository.save(atualizado);
        }
    }

    public void devolverEstoque(Pedido pedido) {
        for (Produto produto : pedido.getProdutos()) {
            Produto atualizado = produtoService.buscarPorId(produto.getId());
            atualizado.setEstoque(atualizado.getEstoque() + 1);
            produtoRepository.save(atualizado);
        }
    }
}
