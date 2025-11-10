package backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée principal de l'application Spring Boot.
 *
 * <p>Cette classe lance le contexte Spring, initialise les beans et démarre
 * le serveur web embarqué (généralement Tomcat).</p>
 *
 * <p>L’annotation {@link SpringBootApplication} combine trois annotations :</p>
 * <ul>
 *   <li>{@code @Configuration} – indique que cette classe contient la configuration Spring principale ;</li>
 *   <li>{@code @EnableAutoConfiguration} – active la configuration automatique de Spring Boot ;</li>
 *   <li>{@code @ComponentScan} – scanne le package courant et ses sous-packages pour détecter les composants Spring.</li>
 * </ul>
 *
 * <p>L’annotation {@link EnableScheduling} permet d’activer l’exécution
 * planifiée de tâches (méthodes annotées avec {@code @Scheduled}).</p>
 *
 * <p>Exécution :</p>
 * <pre>{@code
 * // Démarrage de l’application
 * $ mvn spring-boot:run
 *
 * // Ou en exécutant directement la méthode main :
 * public static void main(String[] args) {
 *     SpringApplication.run(Application.class, args);
 * }
 * }</pre>
 */
@SpringBootApplication
@EnableScheduling // Active la planification de tâches (optionnelle)
public class Application {

    /**
     * Méthode principale appelée au démarrage de l’application.
     *
     * @param args arguments de la ligne de commande (optionnels)
     */
    public static void main(String[] args) {
        // Démarre le contexte Spring et le serveur embarqué (Tomcat par défaut)
        SpringApplication.run(Application.class, args);
    }
}
