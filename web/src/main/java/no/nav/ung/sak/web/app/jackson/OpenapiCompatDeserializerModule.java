package no.nav.ung.sak.web.app.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * jackson Module som aktiverer modifisert deserialisering for enums tilsvarande slik openapi spesifikasjon av disse blir genererert.
 * <p>
 * Sjå {@link ObjectMapperResolver} for bruk.
 * <p>
 * Nødvendig når enums er definert med @JsonFormat(shape = object), @JsonCreator eller andre tilpasninger som gjere at
 * standard serialisering/deserialisering ikkje stemmer med openapi spesifikasjon.
 * <p>
 * Trengs ikkje viss alle enums istaden er definert med @JsonValue eller ingen overstyring av serialisering.
 */
public class OpenapiCompatDeserializerModule extends SimpleModule {
    public OpenapiCompatDeserializerModule() {
        super("OpenapiCompatDeserializerModule");
        this.setDeserializerModifier(new OpenapiEnumBeanDeserializerModifier());
    }
}
