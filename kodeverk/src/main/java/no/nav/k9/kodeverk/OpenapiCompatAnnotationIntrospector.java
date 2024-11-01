package no.nav.k9.kodeverk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.lang.annotation.Annotation;

public class OpenapiCompatAnnotationIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public boolean hasCreatorAnnotation(Annotated a) {
        return super.hasCreatorAnnotation(a);
    }

    @Override
    protected boolean _hasAnnotation(Annotated ann, Class<? extends Annotation> annoClass) {
        // Når annotasjon er på ein enum
        if(ann.getType().isEnumType() || ann.getType().isEnumImplType()) {
            // Deaktiver disse annotasjoner (sidan openapi generator ikkje ser på dei)
            if (annoClass.equals(JsonCreator.class) || annoClass.equals(JsonSerializer.class)) {
                return false;
            }
        }
        return super._hasAnnotation(ann, annoClass);
    }
}
