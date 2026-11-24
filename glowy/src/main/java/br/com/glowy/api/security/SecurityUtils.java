package br.com.glowy.api.security;

import br.com.glowy.api.exception.ForbiddenException;
import br.com.glowy.api.model.Usuario;
import br.com.glowy.api.model.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SecurityUtils {

    public Usuario getUsuarioAutenticado() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new ForbiddenException("Contexto de requisição indisponível.");
        }
        HttpServletRequest request = attrs.getRequest();
        Usuario usuario = (Usuario) request.getAttribute("usuarioAutenticado");
        if (usuario == null) {
            throw new ForbiddenException("Usuário autenticado não encontrado na requisição.");
        }
        return usuario;
    }

    public void validarAdmin() {
        Usuario atual = getUsuarioAutenticado();
        if (atual.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Acesso restrito a administradores.");
        }
    }

    public void validarDonoOuAdmin(Long ownerId) {
        Usuario atual = getUsuarioAutenticado();
        if (atual.getRole() != Role.ADMIN && !atual.getId().equals(ownerId)) {
            throw new ForbiddenException("Você não tem permissão para acessar ou alterar recursos de outro usuário.");
        }
    }
}
