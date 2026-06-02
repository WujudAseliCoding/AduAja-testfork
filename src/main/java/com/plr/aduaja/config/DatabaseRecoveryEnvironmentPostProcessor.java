package com.plr.aduaja.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DatabaseRecoveryEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DOTENV_PROPERTY_SOURCE = "aduaja-dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        loadDotenv(environment);
        DatabaseStartupRecovery.prepareDatabase();
    }

    private void loadDotenv(ConfigurableEnvironment environment) {
        Path dotenvPath = Path.of(".env");
        if (!Files.exists(dotenvPath)) {
            return;
        }
        try {
            Map<String, Object> props = new HashMap<>();
            try (Stream<String> lines = Files.lines(dotenvPath)) {
                lines.map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .filter(line -> line.contains("="))
                        .forEach(line -> {
                            int eq = line.indexOf('=');
                            String key = line.substring(0, eq).trim();
                            String value = line.substring(eq + 1).trim();
                            if (!key.isEmpty()) {
                                props.put(key, value);
                            }
                        });
            }
            if (!props.isEmpty()) {
                MutablePropertySources sources = environment.getPropertySources();
                if (sources.contains(DOTENV_PROPERTY_SOURCE)) {
                    sources.replace(DOTENV_PROPERTY_SOURCE, new MapPropertySource(DOTENV_PROPERTY_SOURCE, props));
                } else {
                    sources.addFirst(new MapPropertySource(DOTENV_PROPERTY_SOURCE, props));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca file .env", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

