package br.com.glowy.api.assembler;

import br.com.glowy.api.controller.CategoriaController;
import br.com.glowy.api.controller.ProdutoController;
import br.com.glowy.api.model.Categoria;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class CategoriaModelAssembler implements RepresentationModelAssembler<Categoria, EntityModel<Categoria>> {

    @Override
    public EntityModel<Categoria> toModel(Categoria categoria) {
        return EntityModel.of(categoria,
                linkTo(methodOn(CategoriaController.class).buscarPorId(categoria.getId())).withSelfRel(),
                linkTo(methodOn(CategoriaController.class).listar(null, null)).withRel("categorias"),
                linkTo(methodOn(ProdutoController.class).listarPorCategoria(categoria.getId(), null, null)).withRel("produtos-da-categoria"),
                linkTo(methodOn(CategoriaController.class).deletar(categoria.getId())).withRel("delete")
        );
    }
}
