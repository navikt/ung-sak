package no.nav.k9.kodeverk;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.k9.kodeverk.api.Kodeverdi;

import java.io.IOException;

/**
 * Serialiserer enum instanser slik at dei er kompatible med standard Openapi tolkning.
 * Det vil seie at vanlege klasser blir serialisert som vanleg i java, mens enums blir serialisert som
 * string basert på @JsonValue viss den er satt, toString() viss ikkje.
 */
public class OpenapiEnumSerializer extends StdSerializer<Enum> {
    private final ObjectMapper baseObjectMapper;

    /**
     * @param baseObjectMapper brukast til å forsøke å serialisere enums på standard måte for å sjå om
     */
    public OpenapiEnumSerializer(final ObjectMapper baseObjectMapper){
        super(Enum.class);
        this.baseObjectMapper = baseObjectMapper;
    }

    @Override
    public void serialize(Enum value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if(value.getClass().isEnum()) {
            // Hent ut kva verdien vil bli viss ein bruke standard serialisering, så vi kan sjekke om den er kompatibel med Openapi definisjon.
            final JsonNode defaultValue = this.baseObjectMapper.valueToTree(value);
            // Standard serialiseringsfunksjonalitet er kompatibel viss den resulterer i String verdi, eller ikkje er satt.
            final boolean defaultValueIsOpenapiCompatible = defaultValue.isTextual() || defaultValue.isNull();
            if(!defaultValueIsOpenapiCompatible) {
                if (defaultValue.isObject()) {
                    // Enum er annotert med @JsonFormat shape = OBJECT og har ingen @JsonValue annotasjon. Den
                    // blir derfor serialisert som objekt. Dette er inkompatibelt med generert openapi spesifikasjon.
                    // @JsonFormat burde fjernast og erstattast med @JsonValue slik at den blir kompatibel med openapi.
                    // For at serialisering skal stemme med generert openapi spesifikasjon, bruk resultat av å kalle
                    // toString() på verdien som json string, sidan dette er det Openapi definisjonen har blitt generert
                    // med i dette tilfellet.
                    gen.writeString(value.toString());
                } else if (defaultValue.isNumber()) {
                    // Enum er annotert med @JsonValue på ein prop med talverdi. Vil i utgangspunktet bli serialisert
                    // som javascript nummer, men Openapi spesifikasjon vil ha blitt generert med verdien konvertert til string.
                    // Konverterer derfor verdi til string her og. Kjenner ikkje til Kodeverdi typer som har slik sammenstilling.
                    gen.writeString(defaultValue.numberValue().toString());
                } else {
                    // Kjenner ikkje til at det eksisterer enums i koden som resulterer i andre json typer etter
                    // serialisering. Kaster feil viss det skjer, slik at vi kan oppdage og fikse evt tilfeller istadenfor
                    // å returnere inkompatibelt resultat.
                    throw new RuntimeException("enum ville blitt serialisert til " + defaultValue.getNodeType().toString() + ". Dette er ikkje kompatibelt med Openapi definisjon, og konvertering er ikkje støtta.");
                }
            } else {
                gen.writeTree(defaultValue);
            }
        } else {
            provider.defaultSerializeValue(value, gen);
        }
    }
}
