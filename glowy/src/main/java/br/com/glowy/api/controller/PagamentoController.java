package br.com.glowy.api.controller;

import br.com.glowy.api.assembler.PagamentoModelAssembler;
import br.com.glowy.api.dto.PagamentoRequestDTO;
import br.com.glowy.api.model.Pagamento;
import br.com.glowy.api.model.enums.StatusPagamento;
import br.com.glowy.api.service.PagamentoService;
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
@RequestMapping("/api/pagamentos")
@Tag(name = "5. Pagamentos", description = "Processamento e consulta de pagamentos dos pedidos (One-to-One com Pedido)")
public class PagamentoController {

    private final PagamentoService service;
    private final PagamentoModelAssembler assembler;

    public PagamentoController(PagamentoService service, PagamentoModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Registrar e aprovar pagamento de um pedido (Dono ou ADMIN)")
    public ResponseEntity<EntityModel<Pagamento>> registrar(@Valid @RequestBody PagamentoRequestDTO dto) {
        Pagamento pago = service.registrarPagamento(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toModel(pago));
    }

    @GetMapping
    @Operation(summary = "Listar pagamentos paginados (Exclusivo ADMIN)")
    public ResponseEntity<PagedModel<EntityModel<Pagamento>>> listar(
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Pagamento> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.listar(pageable), assembler));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pagamento por ID (Dono ou ADMIN)")
    public ResponseEntity<EntityModel<Pagamento>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.buscarPorId(id)));
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Consulta personalizada: Buscar pagamento pelo ID do pedido (Dono ou ADMIN)")
    public ResponseEntity<EntityModel<Pagamento>> buscarPorPedido(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(assembler.toModel(service.buscarPorPedidoId(pedidoId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Consulta personalizada: Filtrar pagamentos por status (paginado, Exclusivo ADMIN)")
    public ResponseEntity<PagedModel<EntityModel<Pagamento>>> listarPorStatus(
            @PathVariable StatusPagamento status,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Pagamento> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.buscarPorStatus(status, pageable), assembler));
    }

    @PutMapping("/{id}/estornar")
    @Operation(summary = "Estornar pagamento e cancelar pedido (Dono ou ADMIN)")
    public ResponseEntity<EntityModel<Pagamento>> estornar(@PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.estornar(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover registro de pagamento (Exclusivo ADMIN)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
