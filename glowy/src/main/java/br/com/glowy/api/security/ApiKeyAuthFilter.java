package br.com.glowy.api.security;

import br.com.glowy.api.model.Usuario;
import br.com.glowy.api.model.enums.Role;
import br.com.glowy.api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Pattern ROTATE_KEY_PATTERN = Pattern.compile("^/api/usuarios/(\\d+)/api-key$");
    private static final int MAX_FALHAS_AUTH_POR_MINUTO = 10;
    private static final long JANELA_SEGUNDOS = 60L;

    private record JanelaFalhas(long inicioJanela, int falhas) {}

    private final UsuarioRepository usuarioRepository;
    private final ConcurrentHashMap<String, JanelaFalhas> falhasPorIp = new ConcurrentHashMap<>();

    public ApiKeyAuthFilter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console")
                || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Rotas públicas: cadastro de novo cliente e leitura do catálogo
        boolean isCadastroUsuario = path.equals("/api/usuarios") && "POST".equalsIgnoreCase(method);
        boolean isLeituraCatalogo = (path.startsWith("/api/produtos") || path.startsWith("/api/categorias"))
                && "GET".equalsIgnoreCase(method);

        if (isCadastroUsuario || isLeituraCatalogo) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        long agora = Instant.now().getEpochSecond();

        // Verifica se o IP já está bloqueado por excesso de tentativas inválidas antes de consultar o banco
        JanelaFalhas janelaAtual = falhasPorIp.get(ip);
        if (janelaAtual != null
                && (agora - janelaAtual.inicioJanela()) < JANELA_SEGUNDOS
                && janelaAtual.falhas() >= MAX_FALHAS_AUTH_POR_MINUTO) {
            long retryAfter = Math.max(1, JANELA_SEGUNDOS - (agora - janelaAtual.inicioJanela()));
            enviarBloqueioForcaBruta(response, retryAfter);
            return;
        }

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            registrarFalhaAuth(ip, agora, response, "Chave de API ausente. Informe o cabeçalho X-API-Key.");
            return;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByApiKey(apiKey);
        if (usuarioOpt.isEmpty()) {
            registrarFalhaAuth(ip, agora, response, "Chave de API inválida.");
            return;
        }

        Usuario autenticado = usuarioOpt.get();
        request.setAttribute("usuarioAutenticado", autenticado);

        // Escrita em Categorias ou Produtos exige perfil ADMIN
        boolean isEscritaCatalogo = (path.startsWith("/api/produtos") || path.startsWith("/api/categorias"))
                && !"GET".equalsIgnoreCase(method);
        if (isEscritaCatalogo && autenticado.getRole() != Role.ADMIN) {
            enviarErro(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                    "Apenas administradores podem alterar categorias ou produtos.");
            return;
        }

        // Rotação de API Key exige ser o próprio usuário ou ADMIN
        Matcher matcher = ROTATE_KEY_PATTERN.matcher(path);
        if (matcher.matches() && "POST".equalsIgnoreCase(method)) {
            Long targetUserId = Long.parseLong(matcher.group(1));
            if (autenticado.getRole() != Role.ADMIN && !autenticado.getId().equals(targetUserId)) {
                enviarErro(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                        "Você só pode gerar uma nova chave para a sua própria conta.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void registrarFalhaAuth(String ip, long agora, HttpServletResponse response, String mensagem) throws IOException {
        if (falhasPorIp.size() > 1000) {
            falhasPorIp.entrySet().removeIf(entry -> (agora - entry.getValue().inicioJanela()) >= JANELA_SEGUNDOS);
        }

        JanelaFalhas atual = falhasPorIp.compute(ip, (k, existente) -> {
            if (existente == null || (agora - existente.inicioJanela()) >= JANELA_SEGUNDOS) {
                return new JanelaFalhas(agora, 1);
            }
            return new JanelaFalhas(existente.inicioJanela(), existente.falhas() + 1);
        });

        if (atual.falhas() >= MAX_FALHAS_AUTH_POR_MINUTO) {
            long retryAfter = Math.max(1, JANELA_SEGUNDOS - (agora - atual.inicioJanela()));
            enviarBloqueioForcaBruta(response, retryAfter);
            return;
        }

        enviarErro(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", mensagem);
    }

    private void enviarBloqueioForcaBruta(HttpServletResponse response, long retryAfter) throws IOException {
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        enviarErro(response, 429, "Too Many Requests",
                "Muitas tentativas de autenticação inválidas. Aguarde o tempo indicado em Retry-After.");
    }

    private void enviarErro(HttpServletResponse response, int status, String erro, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {
                  "status": %d,
                  "erro": "%s",
                  "mensagem": "%s"
                }
                """.formatted(status, erro, mensagem));
    }
}
