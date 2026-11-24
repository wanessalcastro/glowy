package br.com.glowy.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI glowyOpenAPI() {
        final String securitySchemeName = "ApiKeyAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Glowy API ")
                        .version("v2.0")
                        .description("Glowy API — Plataforma RESTful do clube de assinatura e e-commerce da Glowy Box, responsável por gerenciar clientes, curadoria de boxes de maquiagem, controle de estoque, pedidos idempotentes e pagamentos.")
                        .contact(new Contact().name("Equipe Glowy").email("suporte@glowy.com.br")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name("X-API-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Chaves de teste: CLIENTE = glowy-dev-key-123 | ADMIN = glowy-admin-key-999")));
    }
}
