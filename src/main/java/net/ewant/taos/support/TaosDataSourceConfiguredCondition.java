package net.ewant.taos.support;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Locale;

/** Activates the legacy TAOS integration only when its namespace is configured. */
public class TaosDataSourceConfiguredCondition extends SpringBootCondition {

    private static final String NORMALIZED_PREFIX = "spring.datasource.taos.";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource) {
                for (String name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                    if (normalize(name).startsWith(NORMALIZED_PREFIX)) {
                        return ConditionOutcome.match("TAOS datasource properties are present");
                    }
                }
            }
        }
        return ConditionOutcome.noMatch("No TAOS datasource properties are configured");
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace('_', '.').replace("-", "");
    }
}
