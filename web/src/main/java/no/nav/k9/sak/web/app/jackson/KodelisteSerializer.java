package no.nav.k9.sak.web.app.jackson;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.k9.kodeverk.api.Kodeverdi;

/**
 * Enkel serialisering av KodeverkTabell klasser, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
 * av seg selv normalt (får null for andre felter).
 */
public class KodelisteSerializer extends StdSerializer<Kodeverdi> {

    public static final String KODE = "kode";
    public static final String NAVN = "navn";
    public static final String KODEVERK = "kodeverk";
    private final boolean serialiserKodelisteNavn;
    private final boolean serialiserSomString;

    /**
     * dropper navn hvis false (trenger da ikke refreshe navn fra db.). Default false
     */
    private Set<String> reserverteKeys = Set.of(KODE, KODEVERK, NAVN);

    public KodelisteSerializer(final boolean serialiserKodelisteNavn, final boolean serialiserSomString) {
        super(Kodeverdi.class);
        if(serialiserKodelisteNavn && serialiserSomString) {
            throw new IllegalArgumentException("serialiserKodelisteNavn kan ikke vere true når serialiserSomString er true. Må serialisere kodeverk som objekt for å kunne inkludere navn.");
        }
        this.serialiserKodelisteNavn = serialiserKodelisteNavn;
        this.serialiserSomString = serialiserSomString;
    }

    @Override
    public void serialize(Kodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (serialiserSomString) {
            serialisertKodeverdiSomStreng(value, jgen);
        } else {
            serialiserKodeverdiSomObjekt(value, jgen);
        }
    }

    private void serialisertKodeverdiSomStreng(Kodeverdi value, JsonGenerator jgen) throws IOException {
        jgen.writeString(value.getKode());
    }

    private void serialiserKodeverdiSomObjekt(Kodeverdi value, JsonGenerator jgen) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField(KODE, value.getKode());
        if (serialiserKodelisteNavn) {
            jgen.writeStringField(NAVN, value.getNavn());
        }
        jgen.writeStringField(KODEVERK, value.getKodeverk());
        håndtereEkstraFelter(value, jgen);
        jgen.writeEndObject();
    }

    private void håndtereEkstraFelter(Kodeverdi value, JsonGenerator jgen) throws IOException {
        var ekstraFelterMap = value.getEkstraFelter();
        if (ekstraFelterMap == null || ekstraFelterMap.isEmpty()) {
            return;
        }
        var ekstraFelter = ekstraFelterMap
            .entrySet()
            .stream()
            .filter(it -> Objects.nonNull(it.getKey()))
            .filter(it -> !it.getKey().trim().isEmpty())
            .filter(it -> !reserverteKeys.contains(it.getKey().trim()))
            .collect(Collectors.toSet());

        for (Map.Entry<String, String> it : ekstraFelter) {
            jgen.writeStringField(it.getKey(), it.getValue());
        }
    }

}
