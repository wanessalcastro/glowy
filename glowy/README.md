# Glowy API — Clube & Loja de Boxes de Maquiagem

Java 17 · Spring Boot 4.2.0 · JPA/H2 · HATEOAS · Springdoc 3.x (Swagger)

## Como executar
```bash
./mvnw spring-boot:run
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- Console H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:glowydb`, user `sa`, sem senha)

## Chaves de teste (botão **Authorize** no Swagger, cabeçalho `X-API-Key`)
| Perfil  | Chave                  |
|---------|------------------------|
| CLIENTE | `glowy-dev-key-123`    |
| ADMIN   | `glowy-admin-key-999`  |

## Recursos implementados
- 5 entidades (Usuario, Categoria, Produto, Pedido, Pagamento) com relações One-to-One, One-to-Many e Many-to-Many
- Bean Validation, tratamento global de erros (400/401/403/404/409/422/429)
- HATEOAS (EntityModel/PagedModel), paginação e consultas personalizadas
- Autenticação por `X-API-Key` com perfis ADMIN/CLIENTE e checagem de dono do recurso
- Rate limit (5 compras/min e 60 req/min por usuário; por IP para anônimos) com `Retry-After`
- Proteção contra força bruta de chave (10 falhas/min por IP → 429)
- Idempotência em `POST /api/pedidos` (`X-Idempotency-Key`, escopo por usuário, hash do corpo, gravação na mesma transação)
- Versionamento via `X-API-Version` (1 ou 2) em `GET /api/produtos`
- Controle de estoque com `@Version` (lock otimista), devolução em cancelamento/estorno
- CORS configurado

## Regras de status do pedido
- `PAGO` só é definido ao registrar o pagamento (`POST /api/pagamentos`).
- Cancelar: dono ou ADMIN, nunca após envio e nunca com pagamento aprovado (use `PUT /api/pagamentos/{id}/estornar`).
- `CANCELADO` só volta para `AGUARDANDO_PAGAMENTO` (ADMIN), rebaixando o estoque; depois pode ser pago de novo.
- `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE` exigem pagamento aprovado (ADMIN).

## Limitações conhecidas (documentadas de propósito)
1. **Bloqueio por IP:** o limite de falhas de autenticação é por IP. Vários usuários atrás do mesmo NAT/proxy compartilham o contador; um deles errando chaves pode bloquear os demais por até 1 minuto.
2. **Replay de idempotência:** devolve o pedido no seu estado *atual* (pode estar pago/cancelado), com o status HTTP original (201) e o cabeçalho `X-Idempotent-Replay: true`.
3. Estado do rate limit e do contador de falhas fica em memória (uma única instância). Dados em H2 em memória são perdidos ao reiniciar.
4. Se a requisição perdedora de uma corrida de idempotência chegar antes do commit da vencedora, ela recebe 409; o cliente deve tentar de novo e então receberá o replay.

## Roteiro rápido de testes
1. `GET /api/produtos` (sem chave) → 200; com `X-API-Version: 2` → campos extras; com `X-API-Version: 9` → 400.
2. `POST /api/pedidos` com CLIENTE + `X-Idempotency-Key: a1` → 201; repetir igual → 201 + `X-Idempotent-Replay: true`; mesma chave com corpo diferente → 422.
3. Com a chave do CLIENTE, `GET /api/pedidos` → 403 (só ADMIN).
4. Com CLIENTE, `POST /api/produtos` → 403; com ADMIN → 201.
5. `POST /api/pagamentos` → 201 e pedido `PAGO`; `PUT /api/pedidos/{id}/status?status=CANCELADO` → 422 (estorne antes); `PUT /api/pagamentos/{id}/estornar` → 200 e estoque devolvido.
6. 6 `POST /api/pedidos` em 1 minuto → 429 com `Retry-After`.
7. 11 requisições com `X-API-Key` inválida em 1 minuto → 429.

## Visual do Swagger UI
O Swagger UI usa um tema próprio da Glowy (rosa/lilás, tipografia Poppins, cantos arredondados).
- CSS editável: `src/main/resources/swagger/glowy-swagger.css`
- Servido por: `config/SwaggerCustomCssController.java` (substitui `/swagger-ui/index.css`)
- Para ver mudanças, reinicie a aplicação e recarregue com Ctrl+F5.
