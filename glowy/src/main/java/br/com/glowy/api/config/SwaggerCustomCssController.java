package br.com.glowy.api.config;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Documentação da API com o tema da Glowy, em http://localhost:8080/docs
 *  - /docs            -> página do Swagger UI com o tema aplicado
 *  - /docs/tema.css   -> CSS editável em src/main/resources/swagger/glowy-swagger.css
 * Usa uma URL própria (/docs) para não conflitar com as rotas do Springdoc;
 * os scripts e o swagger-ui.css continuam vindo do Springdoc (/swagger-ui/**),
 * e o Swagger original segue disponível em /swagger-ui.html.
 */
@Hidden
@RestController
public class SwaggerCustomCssController {

    private static final String PAGINA = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Glowy API - Documentação</title>
              <link rel="icon" href="data:image/svg+xml,<svg xmlns=%27http://www.w3.org/2000/svg%27 viewBox=%270 0 100 100%27><text y=%27.9em%27 font-size=%2790%27>✨</text></svg>">
              <link rel="stylesheet" type="text/css" href="/swagger-ui/swagger-ui.css">
              <link rel="stylesheet" type="text/css" href="/docs/tema.css">
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="/swagger-ui/swagger-ui-bundle.js"></script>
              <script src="/swagger-ui/swagger-ui-standalone-preset.js"></script>
              <script>
                window.onload = function () {
                  window.ui = SwaggerUIBundle({
                    configUrl: "/v3/api-docs/swagger-config",
                    dom_id: "#swagger-ui",
                    deepLinking: true,
                    persistAuthorization: true,
                    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                    layout: "StandaloneLayout"
                  });
                };
              </script>
            </body>
            </html>
            """;

    @GetMapping(path = "/docs", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> pagina() {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(PAGINA);
    }

    @GetMapping(path = "/docs/tema.css", produces = "text/css;charset=UTF-8")
    public ResponseEntity<String> tema() throws IOException {
        ClassPathResource recurso = new ClassPathResource("swagger/glowy-swagger.css");
        String css = StreamUtils.copyToString(recurso.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(css);
    }
}