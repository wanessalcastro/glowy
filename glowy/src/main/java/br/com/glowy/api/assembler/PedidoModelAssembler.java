package br.com.glowy.api.assembler;

import br.com.glowy.api.controller.PagamentoController;
import br.com.glowy.api.controller.PedidoController;
import br.com.glowy.api.controller.UsuarioController;
import br.com.glowy.api.model.Pedido;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class PedidoModelAssembler implements RepresentationModelAssembler<Pedido, EntityModel<Pedido>> {

    @Override
    public EntityModel<Pedido> toModel(Pedido pedido) {
        return EntityModel.of(pedido,
                linkTo(methodOn(PedidoController.class).buscarPorId(pedido.getId())).withSelfRel(),
                linkTo(methodOn(PedidoController.class).listar(null, null)).withRel("pedidos"),
                linkTo(methodOn(UsuarioController.class).buscarPorId(pedido.getUsuario().getId())).withRel("cliente"),
                linkTo(methodOn(PagamentoController.class).buscarPorPedido(pedido.getId())).withRel("pagamento"),
                linkTo(methodOn(PedidoController.class).deletar(pedido.getId())).withRel("delete")
        );
    }
}
