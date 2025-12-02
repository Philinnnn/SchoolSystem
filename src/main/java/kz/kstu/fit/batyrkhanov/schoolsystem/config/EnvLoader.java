package kz.kstu.fit.batyrkhanov.schoolsystem.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads environment variables from .env early in bootstrap.
 * Puts them into Spring Environment so ${VAR} placeholders resolve without defaults.
 */
public class EnvLoader implements EnvironmentPostProcessor, Ordered {
    private static final Logger log = Logger.getLogger(EnvLoader.class.getName());

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = new File(".env");
        Dotenv dotenv;
        if (envFile.exists()) {
            log.info("Loading variables from .env");
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        } else if (new File(".env.dist").exists()) {
            log.info(".env not found. Loading variables from .env.dist");
            dotenv = Dotenv.configure().filename(".env.dist").ignoreIfMissing().load();
        } else {
            log.warning("No .env or .env.dist file found. Environment variables must be provided externally.");
            return;
        }
        Map<String, Object> props = new HashMap<>();
        dotenv.entries().forEach(e -> {
            String key = e.getKey();
            String value = e.getValue();
            if (value != null && !value.isBlank()) {
                props.put(key, value);
                // Set as system property to help libraries expecting System.getProperty
                System.setProperty(key, value);
            }
        });
        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenv", props));
            log.info("Loaded " + props.size() + " variables into Environment (source 'dotenv').");
        } else {
            log.warning("No non-empty variables loaded from env file.");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
