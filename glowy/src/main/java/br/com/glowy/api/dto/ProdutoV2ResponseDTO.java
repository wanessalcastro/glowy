package br.com.glowy.api.dto;

import br.com.glowy.api.model.Produto;
import java.math.BigDecimal;

public record ProdutoV2ResponseDTO(
        String versaoApi,
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer estoque,
        String statusEstoque,
        int pontosFidelidadeGlowy,
        String seloCuradoria,
        String categoriaNome
) {
    public static ProdutoV2ResponseDTO fromProduto(Produto p) {
        String status = p.getEstoque() > 10 ? "DISPONIVEL" : (p.getEstoque() > 0 ? "ULTIMAS_UNIDADES" : "ESGOTADO");
        int pontos = p.getPreco().intValue() * 2;
        String selo = p.isEdicaoAssinatura() ? "Clube Glowy Exclusivo" : "Loja Avulsa";
        return new ProdutoV2ResponseDTO(
                "v2",
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                p.getPreco(),
                p.getEstoque(),
                status,
                pontos,
                selo,
                p.getCategoria().getNome()
        );
    }
}
