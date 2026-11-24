package br.com.glowy.api.service;

import br.com.glowy.api.dto.ApiKeyResponseDTO;
import br.com.glowy.api.dto.UsuarioRequestDTO;
import br.com.glowy.api.exception.BusinessException;
import br.com.glowy.api.exception.ResourceNotFoundException;
import br.com.glowy.api.model.Usuario;
import br.com.glowy.api.repository.UsuarioRepository;
import br.com.glowy.api.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final SecurityUtils securityUtils;

    public UsuarioService(UsuarioRepository repository, SecurityUtils securityUtils) {
        this.repository = repository;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public Usuario criar(UsuarioRequestDTO dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new BusinessException("Já existe uma cliente cadastrada com o e-mail: " + dto.email());
        }
        Usuario u = new Usuario();
        u.setNome(dto.nome());
        u.setEmail(dto.email());
        u.setTomPele(dto.tomPele());
        u.setApiKey("glowy-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        return repository.save(u);
    }

    public Page<Usuario> listar(Pageable pageable) {
        securityUtils.validarAdmin();
        return repository.findAll(pageable);
    }

    public Usuario buscarPorId(Long id) {
        securityUtils.validarDonoOuAdmin(id);
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
    }

    public Page<Usuario> buscarPorNome(String nome, Pageable pageable) {
        securityUtils.validarAdmin();
        return repository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario existente = buscarPorId(id);
        existente.setNome(dto.nome());
        existente.setEmail(dto.email());
        existente.setTomPele(dto.tomPele());
        return repository.save(existente);
    }

    @Transactional
    public void deletar(Long id) {
        Usuario existente = buscarPorId(id);
        repository.delete(existente);
    }

    @Transactional
    public ApiKeyResponseDTO gerarNovaApiKey(Long id) {
        Usuario usuario = buscarPorId(id);
        String novaChave = "glowy-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        usuario.setApiKey(novaChave);
        repository.save(usuario);
        return new ApiKeyResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                novaChave,
                "Envie esta chave no cabeçalho X-API-Key nas rotas protegidas."
        );
    }
}
