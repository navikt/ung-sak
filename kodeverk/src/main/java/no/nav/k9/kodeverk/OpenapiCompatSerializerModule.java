package no.nav.k9.kodeverk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * jackson Module som serialiserer og deserialiserer enums tilsvarande slik openapi spesifikasjon av disse blir genererert.
 * <p>
 * Nødvendig når enums er definert med @JsonFormat(shape = object), @JsonCreator eller andre tilpasninger som gjere at
 * standard serialisering/deserialisering ikkje stemmer med openapi spesifikasjon.
 * <p>
 * Trengs ikkje viss alle enums istaden er definert med @JsonValue eller ingen overstyring av serialisering.
 */
public class OpenapiCompatSerializerModule extends SimpleModule {
    public OpenapiCompatSerializerModule(final ObjectMapper baseObjectMapper) {
        super("OpenapiCompatSerializerModule");
        this.addSerializer(new OpenapiEnumSerializer(baseObjectMapper));
        this.setDeserializerModifier(new OpenapiEnumBeanDeserializerModifier());
    }
}
