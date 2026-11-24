package br.com.glowy.api.security;

import br.com.glowy.api.model.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_COMPRAS_POR_MINUTO = 5;
    private static final int LIMITE_GERAL_POR_MINUTO = 60;
    private static final long JANELA_SEGUNDOS = 60L;

    private record JanelaRequisicoes(long inicioJanela, int contador) {}

    private final ConcurrentHashMap<String, JanelaRequisicoes> clientes = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean rotaCompra = path.startsWith("/api/pedidos") && "POST".equalsIgnoreCase(request.getMethod());
        int limite = rotaCompra ? LIMITE_COMPRAS_POR_MINUTO : LIMITE_GERAL_POR_MINUTO;

        // Só usa ID de usuário se a chave já tiver sido autenticada; nunca confia no header cru
        Usuario autenticado = (Usuario) request.getAttribute("usuarioAutenticado");
        String identificador = (autenticado != null)
                ? "USER:" + autenticado.getId()
                : "IP:" + request.getRemoteAddr();

        String chaveBalde = identificador + ":" + (rotaCompra ? "PEDIDOS" : "GERAL");
        long agora = Instant.now().getEpochSecond();

        if (clientes.size() > 1000) {
            clientes.entrySet().removeIf(entry -> (agora - entry.getValue().inicioJanela()) >= JANELA_SEGUNDOS);
        }

        JanelaRequisicoes atual = clientes.compute(chaveBalde, (k, existente) -> {
            if (existente == null || (agora - existente.inicioJanela()) >= JANELA_SEGUNDOS) {
                return new JanelaRequisicoes(agora, 1);
            }
            return new JanelaRequisicoes(existente.inicioJanela(), existente.contador() + 1);
        });

        int restantes = Math.max(0, limite - atual.contador());
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(limite));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(restantes));

        if (atual.contador() > limite) {
            long retryAfter = Math.max(1, JANELA_SEGUNDOS - (agora - atual.inicioJanela()));
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {
                      "status": 429,
                      "erro": "Too Many Requests",
                      "mensagem": "Limite de requisições excedido. Aguarde o tempo indicado em Retry-After."
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
