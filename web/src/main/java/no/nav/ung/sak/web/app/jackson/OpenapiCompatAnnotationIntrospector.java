package no.nav.ung.sak.web.app.jackson;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.lang.annotation.Annotation;

public class OpenapiCompatAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    protected <A extends Annotation> A _findAnnotation(Annotated ann, Class<A> annoClass) {
        final JavaType typ = ann.getType();
        // Når annotasjon er på ein enum
        if(typ != null && (typ.isEnumType() || typ.isTypeOrSubTypeOf(Enum.class))) {
            // Deaktiver alle annotasjoner utanom @JsonValue. Det er den einaste openapi generator bryr seg om.
            if(!annoClass.equals(JsonValue.class)) {
                return null;
            }
        }
        return super._findAnnotation(ann, annoClass);
    }
}
