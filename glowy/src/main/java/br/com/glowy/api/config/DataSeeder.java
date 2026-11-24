package br.com.glowy.api.config;

import br.com.glowy.api.model.*;
import br.com.glowy.api.model.enums.MetodoPagamento;
import br.com.glowy.api.model.enums.Role;
import br.com.glowy.api.model.enums.StatusPagamento;
import br.com.glowy.api.model.enums.StatusPedido;
import br.com.glowy.api.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final String adminKey;
    private final String clienteKey;

    public DataSeeder(UsuarioRepository usuarioRepository, CategoriaRepository categoriaRepository,
                      ProdutoRepository produtoRepository, PedidoRepository pedidoRepository,
                      PagamentoRepository pagamentoRepository,
                      @Value("${glowy.admin-key}") String adminKey,
                      @Value("${glowy.cliente-key}") String clienteKey) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.adminKey = adminKey;
        this.clienteKey = clienteKey;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) return;

        Usuario admin = new Usuario();
        admin.setNome("Admin Glowy");
        admin.setEmail("admin@glowy.com.br");
        admin.setTomPele("Todos");
        admin.setRole(Role.ADMIN);
        admin.setApiKey(adminKey);
        usuarioRepository.save(admin);

        Usuario cliente = new Usuario();
        cliente.setNome("Camila Andrade");
        cliente.setEmail("camila@glowy.com.br");
        cliente.setTomPele("Média Quente");
        cliente.setRole(Role.CLIENTE);
        cliente.setApiKey(clienteKey);
        usuarioRepository.save(cliente);

        Categoria catBoxes = new Categoria();
        catBoxes.setNome("Boxes Mensais");
        catBoxes.setDescricao("Caixas de assinatura mensal com curadoria de maquiagem");
        categoriaRepository.save(catBoxes);

        Categoria catLabios = new Categoria();
        catLabios.setNome("Lábios");
        catLabios.setDescricao("Batons, glosses, lip tints e hidratantes labiais");
        categoriaRepository.save(catLabios);

        Produto box1 = new Produto();
        box1.setNome("Glowy Box Edição Velvet Glam");
        box1.setDescricao("Box mensal com 5 produtos full-size de acabamento aveludado");
        box1.setPreco(new BigDecimal("119.90"));
        box1.setEstoque(30);
        box1.setEdicaoAssinatura(true);
        box1.setCategoria(catBoxes);
        produtoRepository.save(box1);

        Produto gloss = new Produto();
        gloss.setNome("Gloss Labial Honey Glow");
        gloss.setDescricao("Gloss hidratante com partículas douradas e ácido hialurônico");
        gloss.setPreco(new BigDecimal("49.90"));
        gloss.setEstoque(8);
        gloss.setEdicaoAssinatura(false);
        gloss.setCategoria(catLabios);
        produtoRepository.save(gloss);

        Pedido pedidoInicial = new Pedido();
        pedidoInicial.setUsuario(cliente);
        pedidoInicial.setProdutos(new ArrayList<>(List.of(box1, gloss)));
        pedidoInicial.setEnderecoEntrega("Av. Paulista, 1500 - São Paulo, SP");
        pedidoInicial.setValorTotal(new BigDecimal("169.80"));
        pedidoInicial.setStatus(StatusPedido.PAGO);
        pedidoRepository.save(pedidoInicial);

        Pagamento pagInicial = new Pagamento();
        pagInicial.setPedido(pedidoInicial);
        pagInicial.setMetodo(MetodoPagamento.PIX);
        pagInicial.setStatus(StatusPagamento.APROVADO);
        pagInicial.setValor(new BigDecimal("169.80"));
        pagInicial.setCodigoTransacao("TRX-SEED0001");
        pagamentoRepository.save(pagInicial);
    }
}