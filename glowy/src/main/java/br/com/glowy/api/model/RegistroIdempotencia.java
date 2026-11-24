package br.com.glowy.api.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_idempotencia")
public class RegistroIdempotencia implements Persistable<String> {

    @Id
    private String chaveComposta;

    @Column(nullable = false)
    private String payloadHash;

    @Column(nullable = false)
    private Long pedidoId;

    private int statusHttp;

    private LocalDateTime criadoEm = LocalDateTime.now();

    public RegistroIdempotencia() {}

    public RegistroIdempotencia(String chaveComposta, String payloadHash, Long pedidoId, int statusHttp) {
        this.chaveComposta = chaveComposta;
        this.payloadHash = payloadHash;
        this.pedidoId = pedidoId;
        this.statusHttp = statusHttp;
        this.criadoEm = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return chaveComposta;
    }

    // Chave atribuída manualmente: força INSERT (sem SELECT + UPDATE) e dispara violação de PK em corrida
    @Transient
    @Override
    public boolean isNew() {
        return true;
    }

    public String getChaveComposta() { return chaveComposta; }
    public void setChaveComposta(String chaveComposta) { this.chaveComposta = chaveComposta; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    public int getStatusHttp() { return statusHttp; }
    public void setStatusHttp(int statusHttp) { this.statusHttp = statusHttp; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
