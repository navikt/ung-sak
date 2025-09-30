package no.nav.ung.kodeverk;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.io.IOException;

public class KodeverdiSomObjektDeserializer extends StdDeserializer<KodeverdiSomObjekt<?>>
    implements ContextualDeserializer {

    private JavaType valueType;

    public KodeverdiSomObjektDeserializer() {
        super(KodeverdiSomObjekt.class);
    }

    private KodeverdiSomObjektDeserializer(JavaType valueType) {
        super(KodeverdiSomObjekt.class);
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType contextualType = ctxt.getContextualType();
        if (contextualType != null && contextualType.hasGenericTypes()) {
            // Extract the generic type parameter (e.g., DokumentMalType from KodeverdiSomObjekt<DokumentMalType>)
            return new KodeverdiSomObjektDeserializer(contextualType.containedType(0));
        }
        return this;
    }

    @Override
    public KodeverdiSomObjekt<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String kode = node.get("kode").asText();

        if (valueType != null ) {
            if(valueType.getRawClass().isEnum()) {
                Class<?> enumClass = valueType.getRawClass();
                for (Object enumValue : enumClass.getEnumConstants()) {
                    Kodeverdi kodeverdi = (Kodeverdi) enumValue;
                    if (kodeverdi.getKode().equals(kode)) {
                        return new KodeverdiSomObjekt<>(kodeverdi);
                    }
                }
            } else {
                throw new IllegalStateException("Could not deserialize KodeverdiSomObjekt with kode: \"" + kode + "\". Generic argument " + valueType.getGenericSignature() + " is not an enum");
            }
        }
        throw new IllegalStateException("Could not deserialize KodeverdiSomObjekt with kode: " + kode + ". No generic argument found.");
    }
}
