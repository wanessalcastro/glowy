package br.com.glowy.api.controller;

import br.com.glowy.api.assembler.ProdutoModelAssembler;
import br.com.glowy.api.dto.ProdutoRequestDTO;
import br.com.glowy.api.dto.ProdutoV2ResponseDTO;
import br.com.glowy.api.exception.BadRequestException;
import br.com.glowy.api.model.Produto;
import br.com.glowy.api.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "3. Produtos e Boxes", description = "Catálogo de boxes de maquiagem e itens avulsos (versionado via X-API-Version)")
public class ProdutoController {

    private final ProdutoService service;
    private final ProdutoModelAssembler assembler;

    public ProdutoController(ProdutoService service, ProdutoModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo produto/box (Requer X-API-Key de ADMIN)")
    public ResponseEntity<EntityModel<Produto>> criar(@Valid @RequestBody ProdutoRequestDTO dto) {
        Produto criado = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toModel(criado));
    }

    @GetMapping
    @Operation(
            summary = "Listar produtos (Versionado via cabeçalho X-API-Version: 1 ou 2)",
            description = "Versão 1 retorna o catálogo padrão. Versão 2 adiciona statusEstoque, pontosFidelidadeGlowy e seloCuradoria."
    )
    public ResponseEntity<?> listar(
            @Parameter(description = "Versão do contrato da API", schema = @Schema(allowableValues = {"1", "2"}, defaultValue = "1"))
            @RequestHeader(value = "X-API-Version", defaultValue = "1") String version,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Produto> pagedAssemblerV1,
            PagedResourcesAssembler<ProdutoV2ResponseDTO> pagedAssemblerV2) {

        if ("2".equals(version)) {
            Page<ProdutoV2ResponseDTO> pageV2 = service.listar(pageable).map(ProdutoV2ResponseDTO::fromProduto);
            PagedModel<EntityModel<ProdutoV2ResponseDTO>> modelV2 = pagedAssemblerV2.toModel(pageV2, dto ->
                    EntityModel.of(dto, linkTo(methodOn(ProdutoController.class).buscarPorId(dto.id())).withSelfRel())
            );
            return ResponseEntity.ok().header("X-API-Version", "2").body(modelV2);
        }

        if (!"1".equals(version)) {
            throw new BadRequestException("Versão de API não suportada: '" + version + "'. Utilize 1 ou 2.");
        }

        return ResponseEntity.ok()
                .header("X-API-Version", "1")
                .body(pagedAssemblerV1.toModel(service.listar(pageable), assembler));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID (Rota pública)")
    public ResponseEntity<EntityModel<Produto>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.buscarPorId(id)));
    }

    @GetMapping("/categoria/{categoriaId}")
    @Operation(summary = "Consulta personalizada: Listar produtos de uma categoria (paginado)")
    public ResponseEntity<PagedModel<EntityModel<Produto>>> listarPorCategoria(
            @PathVariable Long categoriaId,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Produto> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.buscarPorCategoria(categoriaId, pageable), assembler));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Consulta personalizada: Buscar produtos por nome (paginado)")
    public ResponseEntity<PagedModel<EntityModel<Produto>>> buscarPorNome(
            @RequestParam String nome,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Produto> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.buscarPorNome(nome, pageable), assembler));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto/box (Requer X-API-Key de ADMIN)")
    public ResponseEntity<EntityModel<Produto>> atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequestDTO dto) {
        return ResponseEntity.ok(assembler.toModel(service.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover produto/box (Requer X-API-Key de ADMIN)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
