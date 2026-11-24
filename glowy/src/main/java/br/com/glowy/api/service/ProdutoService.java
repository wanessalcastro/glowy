package br.com.glowy.api.service;

import br.com.glowy.api.dto.ProdutoRequestDTO;
import br.com.glowy.api.exception.ResourceNotFoundException;
import br.com.glowy.api.model.Categoria;
import br.com.glowy.api.model.Produto;
import br.com.glowy.api.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final CategoriaService categoriaService;

    public ProdutoService(ProdutoRepository repository, CategoriaService categoriaService) {
        this.repository = repository;
        this.categoriaService = categoriaService;
    }

    public Produto criar(ProdutoRequestDTO dto) {
        Categoria categoria = categoriaService.buscarPorId(dto.categoriaId());
        Produto p = new Produto();
        p.setNome(dto.nome());
        p.setDescricao(dto.descricao());
        p.setPreco(dto.preco());
        p.setEstoque(dto.estoque());
        p.setEdicaoAssinatura(dto.edicaoAssinatura());
        p.setCategoria(categoria);
        return repository.save(p);
    }

    public Page<Produto> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Produto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + id));
    }

    public Page<Produto> buscarPorCategoria(Long categoriaId, Pageable pageable) {
        categoriaService.buscarPorId(categoriaId);
        return repository.findByCategoriaId(categoriaId, pageable);
    }

    public Page<Produto> buscarPorNome(String nome, Pageable pageable) {
        return repository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Produto atualizar(Long id, ProdutoRequestDTO dto) {
        Produto existente = buscarPorId(id);
        Categoria categoria = categoriaService.buscarPorId(dto.categoriaId());
        existente.setNome(dto.nome());
        existente.setDescricao(dto.descricao());
        existente.setPreco(dto.preco());
        existente.setEstoque(dto.estoque());
        existente.setEdicaoAssinatura(dto.edicaoAssinatura());
        existente.setCategoria(categoria);
        return repository.save(existente);
    }

    public void deletar(Long id) {
        Produto existente = buscarPorId(id);
        repository.delete(existente);
    }
}
