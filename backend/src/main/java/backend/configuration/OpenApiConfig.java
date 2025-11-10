package backend.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Configuration de la documentation Swagger / OpenAPI pour l’application.
 *
 * <p>Cette classe configure la génération automatique de la documentation
 * interactive de l’API via Swagger UI (accessible sur <code>/swagger-ui.html</code>).</p>
 *
 * <p>Elle intègre également la gestion du schéma de sécurité JWT,
 * afin de permettre la connexion par token directement depuis Swagger.</p>
 *
 * <p><b>Principales annotations :</b></p>
 * <ul>
 *   <li>{@link OpenAPIDefinition} : définit les métadonnées globales de l’API (titre, version, description, sécurité).</li>
 *   <li>{@link SecurityScheme} : décrit le type d’authentification utilisé (ici Bearer Token de type JWT).</li>
 * </ul>
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "OkayoLite API",
        version = "1.0",
        description = "Documentation de l’API OkayoLite sécurisée par JWT"
    ),
    // Indique que la sécurité Bearer s'applique par défaut à toutes les routes
    security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",                  // Nom du schéma utilisé dans Swagger
    scheme = "bearer",                    // Type Bearer (token)
    type = SecuritySchemeType.HTTP,       // Type HTTP (authentification via header)
    bearerFormat = "JWT",                 // Format du token : JSON Web Token
    in = SecuritySchemeIn.HEADER          // Le token est envoyé dans l’en-tête Authorization
)
public class OpenApiConfig {

    /**
     * Définit la configuration OpenAPI personnalisée pour Swagger UI.
     *
     * <p>Cette méthode crée une instance d’{@link OpenAPI} et y ajoute
     * les informations générales (titre, version, description) ainsi que
     * le schéma de sécurité JWT nécessaire pour l’authentification
     * dans l’interface Swagger.</p>
     *
     * @return un objet {@link OpenAPI} configuré pour Swagger UI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            // Informations principales affichées dans Swagger UI
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("OkayoLite API")
                .description("Documentation interactive de l’API OkayoLite protégée par JWT.")
                .version("1.0"))

            // Indique que le schéma de sécurité 'bearerAuth' s'applique
            .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"))

            // Déclare le schéma JWT dans la configuration OpenAPI
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new io.swagger.v3.oas.models.security.SecurityScheme()
                        .name("bearerAuth")
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                )
            );
    }
}
