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
 * Denne brukes til å overstyre standard serialisering av Kodeverdi instanser til å alltid returnere json objekt.
 * Er planlagt utfasa. Alternativ overstyring er KodeverdiSomStringSerializer klassen.
 */
public class KodeverdiSomObjektSerializer extends StdSerializer<Kodeverdi> {

    public static final String KODE = "kode";
    public static final String NAVN = "navn";
    public static final String KODEVERK = "kodeverk";
    private final boolean serialiserKodelisteNavn;

    private Set<String> reserverteKeys = Set.of(KODE, KODEVERK, NAVN);

    /**
     *
     * @param serialiserKodelisteNavn Utelater navn fra serialisering viss denne er false. Standard er false.
     */
    public KodeverdiSomObjektSerializer(final boolean serialiserKodelisteNavn) {
        super(Kodeverdi.class);
        this.serialiserKodelisteNavn = serialiserKodelisteNavn;
    }

    @Override
    public void serialize(Kodeverdi value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        serialiserKodeverdiSomObjekt(value, jgen);
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

    // TODO Forsøk å skrive om så denne kan fjernast. Kun brukt av Venteårsak klasse.
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
