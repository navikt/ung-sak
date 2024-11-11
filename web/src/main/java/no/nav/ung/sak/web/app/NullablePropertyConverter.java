package no.nav.ung.sak.web.app;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Legger til nullable: true i openapi genereringa på alle properties som ikkje spesifikt er annotert med @NotNull.
 *
 * Dette fordi disse properties kan bli satt til null i java, og generert spesifikasjon må reflektere dette for at generert
 * typescript skal generere disse typer korrekt (med | null union type)
 */
public class NullablePropertyConverter implements ModelConverter {
    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if(chain.hasNext()) {
            if(type.isSchemaProperty()) {
                final var result = chain.next().resolve(type, context, chain);
                final var hasNotNull = Arrays.stream(type.getCtxAnnotations()).anyMatch(ann -> ann.annotationType().equals(NotNull.class));
                final boolean isObj = result.getType() != null && result.getType().equalsIgnoreCase("object");
                if(!isObj && !hasNotNull) {
                    result.setNullable(true);
                }
                return result;
            } else {
                return chain.next().resolve(type, context, chain);
            }
        } else {
            return null;
        }
    }
}
