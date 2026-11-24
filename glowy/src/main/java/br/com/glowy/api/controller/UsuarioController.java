package br.com.glowy.api.controller;

import br.com.glowy.api.assembler.UsuarioModelAssembler;
import br.com.glowy.api.dto.ApiKeyResponseDTO;
import br.com.glowy.api.dto.UsuarioRequestDTO;
import br.com.glowy.api.model.Usuario;
import br.com.glowy.api.service.UsuarioService;
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
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "1. Usuários (Clientes)", description = "Gerenciamento de clientes do clube Glowy e emissão de chaves de API")
public class UsuarioController {

    private final UsuarioService service;
    private final UsuarioModelAssembler assembler;

    public UsuarioController(UsuarioService service, UsuarioModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova cliente (Rota pública)", description = "Cria a conta e retorna a API Key inicial (exibida apenas no cadastro ou na rotação).")
    public ResponseEntity<EntityModel<ApiKeyResponseDTO>> criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        Usuario criado = service.criar(dto);
        ApiKeyResponseDTO resposta = new ApiKeyResponseDTO(
                criado.getId(),
                criado.getNome(),
                criado.getApiKey(),
                "Guarde esta chave e envie no cabeçalho X-API-Key."
        );
        EntityModel<ApiKeyResponseDTO> model = EntityModel.of(resposta,
                linkTo(methodOn(UsuarioController.class).buscarPorId(criado.getId())).withSelfRel(),
                linkTo(methodOn(UsuarioController.class).gerarApiKey(criado.getId())).withRel("rotacionar-api-key")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @GetMapping
    @Operation(summary = "Listar clientes paginados (Exclusivo ADMIN, não expõe API Keys)")
    public ResponseEntity<PagedModel<EntityModel<Usuario>>> listar(
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Usuario> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.listar(pageable), assembler));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID (Próprio usuário ou ADMIN)")
    public ResponseEntity<EntityModel<Usuario>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.buscarPorId(id)));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Consulta personalizada: Buscar clientes por nome (paginado, Exclusivo ADMIN)")
    public ResponseEntity<PagedModel<EntityModel<Usuario>>> buscarPorNome(
            @RequestParam String nome,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Usuario> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.buscarPorNome(nome, pageable), assembler));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados da cliente (Próprio usuário ou ADMIN)")
    public ResponseEntity<EntityModel<Usuario>> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(assembler.toModel(service.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cliente (Próprio usuário ou ADMIN)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/api-key")
    @Operation(summary = "Gerar/Renovar chave de API (Requer X-API-Key do próprio usuário ou ADMIN)")
    public ResponseEntity<ApiKeyResponseDTO> gerarApiKey(@PathVariable Long id) {
        return ResponseEntity.ok(service.gerarNovaApiKey(id));
    }
}
