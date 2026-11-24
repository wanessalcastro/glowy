package br.com.glowy.api.controller;

import br.com.glowy.api.assembler.CategoriaModelAssembler;
import br.com.glowy.api.dto.CategoriaRequestDTO;
import br.com.glowy.api.model.Categoria;
import br.com.glowy.api.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "2. Categorias", description = "Categorias de boxes e linhas de maquiagem da Glowy")
public class CategoriaController {

    private final CategoriaService service;
    private final CategoriaModelAssembler assembler;

    public CategoriaController(CategoriaService service, CategoriaModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Criar categoria (Requer X-API-Key de ADMIN)")
    public ResponseEntity<EntityModel<Categoria>> criar(@Valid @RequestBody CategoriaRequestDTO dto) {
        Categoria criada = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toModel(criada));
    }

    @GetMapping
    @Operation(summary = "Listar categorias paginadas (Rota pública)")
    public ResponseEntity<PagedModel<EntityModel<Categoria>>> listar(
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Categoria> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.listar(pageable), assembler));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar categoria por ID (Rota pública)")
    public ResponseEntity<EntityModel<Categoria>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.buscarPorId(id)));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Consulta personalizada: Buscar categorias por nome (paginado)")
    public ResponseEntity<PagedModel<EntityModel<Categoria>>> buscarPorNome(
            @RequestParam String nome,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Categoria> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.buscarPorNome(nome, pageable), assembler));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar categoria (Requer X-API-Key de ADMIN)")
    public ResponseEntity<EntityModel<Categoria>> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequestDTO dto) {
        return ResponseEntity.ok(assembler.toModel(service.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir categoria (Requer X-API-Key de ADMIN)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
