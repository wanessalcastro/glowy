package br.com.glowy.api.controller;

import br.com.glowy.api.assembler.PedidoModelAssembler;
import br.com.glowy.api.dto.PedidoRequestDTO;
import br.com.glowy.api.exception.BadRequestException;
import br.com.glowy.api.model.Pedido;
import br.com.glowy.api.model.RegistroIdempotencia;
import br.com.glowy.api.model.Usuario;
import br.com.glowy.api.model.enums.StatusPedido;
import br.com.glowy.api.security.SecurityUtils;
import br.com.glowy.api.service.IdempotenciaService;
import br.com.glowy.api.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "4. Pedidos", description = "Realização e acompanhamento de pedidos (Protegido por X-API-Key, Rate Limit e Idempotência)")
public class PedidoController {

    private final PedidoService service;
    private final PedidoModelAssembler assembler;
    private final IdempotenciaService idempotenciaService;
    private final SecurityUtils securityUtils;

    public PedidoController(PedidoService service, PedidoModelAssembler assembler,
                            IdempotenciaService idempotenciaService, SecurityUtils securityUtils) {
        this.service = service;
        this.assembler = assembler;
        this.idempotenciaService = idempotenciaService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @Operation(
            summary = "Criar novo pedido (Idempotente via X-Idempotency-Key)",
            description = "Usa o cliente autenticado via X-API-Key. A chave de idempotência tem escopo por usuário, valida o hash do payload e é gravada na mesma transação do pedido. "
                    + "Um replay devolve o pedido no seu estado ATUAL (pode já estar pago/cancelado), com o cabeçalho X-Idempotent-Replay: true."
    )
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave única para evitar pedidos duplicados", example = "pedido-camila-001")
    public ResponseEntity<EntityModel<Pedido>> criar(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PedidoRequestDTO dto) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("O cabeçalho X-Idempotency-Key é obrigatório para criar pedidos.");
        }

        Usuario autenticado = securityUtils.getUsuarioAutenticado();
        String chaveComposta = idempotenciaService.gerarChaveComposta(autenticado.getId(), idempotencyKey);
        String payloadHash = idempotenciaService.calcularHashPayload(dto);

        Optional<RegistroIdempotencia> replay = idempotenciaService.verificarReplay(chaveComposta, payloadHash);
        if (replay.isPresent()) {
            return construirRespostaReplay(replay.get());
        }

        try {
            Pedido criado = service.criar(dto, chaveComposta, payloadHash);
            return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toModel(criado));
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException ex) {
            // Corrida: outra requisição com a mesma chave pode ter vencido (chave duplicada) ou mexido no mesmo
            // estoque (lock otimista). Se a vencedora já commitou, devolve o replay dela; senão relança (409).
            Optional<RegistroIdempotencia> concorrente = idempotenciaService.verificarReplay(chaveComposta, payloadHash);
            if (concorrente.isPresent()) {
                return construirRespostaReplay(concorrente.get());
            }
            throw ex;
        }
    }

    private ResponseEntity<EntityModel<Pedido>> construirRespostaReplay(RegistroIdempotencia reg) {
        Pedido pedidoExistente = service.buscarPorId(reg.getPedidoId());
        return ResponseEntity.status(reg.getStatusHttp())
                .header("X-Idempotent-Replay", "true")
                .body(assembler.toModel(pedidoExistente));
    }

    @GetMapping
    @Operation(summary = "Listar todos os pedidos paginados (Exclusivo ADMIN)")
    public ResponseEntity<PagedModel<EntityModel<Pedido>>> listar(
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Pedido> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.listar(pageable), assembler));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido por ID (Dono do pedido ou ADMIN)")
    public ResponseEntity<EntityModel<Pedido>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(service.buscarPorId(id)));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Consulta personalizada: Listar pedidos de uma cliente (Dono ou ADMIN)")
    public ResponseEntity<PagedModel<EntityModel<Pedido>>> listarPorUsuario(
            @PathVariable Long usuarioId,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Pedido> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.buscarPorUsuario(usuarioId, pageable), assembler));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Consulta personalizada: Filtrar pedidos por status (Exclusivo ADMIN)")
    public ResponseEntity<PagedModel<EntityModel<Pedido>>> listarPorStatus(
            @PathVariable StatusPedido status,
            @ParameterObject Pageable pageable,
            PagedResourcesAssembler<Pedido> pagedAssembler) {
        return ResponseEntity.ok(pagedAssembler.toModel(service.buscarPorStatus(status, pageable), assembler));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Atualizar status do pedido",
            description = "Dono ou ADMIN podem CANCELAR (pedido não pago e não enviado). Demais transições: apenas ADMIN. "
                    + "PAGO só é definido pelo pagamento; um pedido CANCELADO só volta para AGUARDANDO_PAGAMENTO.")
    public ResponseEntity<EntityModel<Pedido>> atualizarStatus(
            @PathVariable Long id,
            @RequestParam StatusPedido status) {
        return ResponseEntity.ok(assembler.toModel(service.atualizarStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar/Excluir pedido e devolver estoque (Dono ou ADMIN)")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
