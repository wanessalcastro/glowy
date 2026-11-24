package br.com.glowy.api.repository;

import br.com.glowy.api.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByApiKey(String apiKey);
    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
