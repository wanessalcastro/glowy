package br.com.glowy.api.assembler;

import br.com.glowy.api.controller.CategoriaController;
import br.com.glowy.api.controller.ProdutoController;
import br.com.glowy.api.model.Produto;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class ProdutoModelAssembler implements RepresentationModelAssembler<Produto, EntityModel<Produto>> {

    @Override
    public EntityModel<Produto> toModel(Produto produto) {
        return EntityModel.of(produto,
                linkTo(methodOn(ProdutoController.class).buscarPorId(produto.getId())).withSelfRel(),
                linkTo(methodOn(ProdutoController.class).listar("1", null, null, null)).withRel("produtos"),
                linkTo(methodOn(CategoriaController.class).buscarPorId(produto.getCategoria().getId())).withRel("categoria"),
                linkTo(methodOn(ProdutoController.class).deletar(produto.getId())).withRel("delete")
        );
    }
}
