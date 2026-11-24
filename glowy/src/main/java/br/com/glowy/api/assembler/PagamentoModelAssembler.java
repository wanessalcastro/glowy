package br.com.glowy.api.assembler;

import br.com.glowy.api.controller.PagamentoController;
import br.com.glowy.api.controller.PedidoController;
import br.com.glowy.api.model.Pagamento;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class PagamentoModelAssembler implements RepresentationModelAssembler<Pagamento, EntityModel<Pagamento>> {

    @Override
    public EntityModel<Pagamento> toModel(Pagamento pagamento) {
        return EntityModel.of(pagamento,
                linkTo(methodOn(PagamentoController.class).buscarPorId(pagamento.getId())).withSelfRel(),
                linkTo(methodOn(PagamentoController.class).listar(null, null)).withRel("pagamentos"),
                linkTo(methodOn(PedidoController.class).buscarPorId(pagamento.getPedidoId())).withRel("pedido"),
                linkTo(methodOn(PagamentoController.class).estornar(pagamento.getId())).withRel("estornar")
        );
    }
}
