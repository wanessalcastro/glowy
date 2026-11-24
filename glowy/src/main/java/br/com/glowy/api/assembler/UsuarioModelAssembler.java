package br.com.glowy.api.assembler;

import br.com.glowy.api.controller.PedidoController;
import br.com.glowy.api.controller.UsuarioController;
import br.com.glowy.api.model.Usuario;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class UsuarioModelAssembler implements RepresentationModelAssembler<Usuario, EntityModel<Usuario>> {

    @Override
    public EntityModel<Usuario> toModel(Usuario usuario) {
        return EntityModel.of(usuario,
                linkTo(methodOn(UsuarioController.class).buscarPorId(usuario.getId())).withSelfRel(),
                linkTo(methodOn(UsuarioController.class).listar(null, null)).withRel("usuarios"),
                linkTo(methodOn(UsuarioController.class).gerarApiKey(usuario.getId())).withRel("gerar-api-key"),
                linkTo(methodOn(PedidoController.class).listarPorUsuario(usuario.getId(), null, null)).withRel("pedidos-cliente"),
                linkTo(methodOn(UsuarioController.class).deletar(usuario.getId())).withRel("delete")
        );
    }
}
