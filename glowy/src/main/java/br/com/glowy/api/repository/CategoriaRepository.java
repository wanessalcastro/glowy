package br.com.glowy.api.repository;

import br.com.glowy.api.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNomeIgnoreCase(String nome);
    Page<Categoria> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
