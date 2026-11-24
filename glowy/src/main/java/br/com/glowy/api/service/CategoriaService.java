package br.com.glowy.api.service;

import br.com.glowy.api.dto.CategoriaRequestDTO;
import br.com.glowy.api.exception.BusinessException;
import br.com.glowy.api.exception.ResourceNotFoundException;
import br.com.glowy.api.model.Categoria;
import br.com.glowy.api.repository.CategoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public Categoria criar(CategoriaRequestDTO dto) {
        if (repository.existsByNomeIgnoreCase(dto.nome())) {
            throw new BusinessException("Já existe uma categoria com o nome: " + dto.nome());
        }
        Categoria c = new Categoria();
        c.setNome(dto.nome());
        c.setDescricao(dto.descricao());
        return repository.save(c);
    }

    public Page<Categoria> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Categoria buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + id));
    }

    public Page<Categoria> buscarPorNome(String nome, Pageable pageable) {
        return repository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Categoria atualizar(Long id, CategoriaRequestDTO dto) {
        Categoria existente = buscarPorId(id);
        existente.setNome(dto.nome());
        existente.setDescricao(dto.descricao());
        return repository.save(existente);
    }

    public void deletar(Long id) {
        Categoria existente = buscarPorId(id);
        repository.delete(existente);
    }
}
