package no.nav.ung.kodeverk.vedtak;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@Deprecated
public enum Vedtaksbrev implements Kodeverdi {

    AUTOMATISK("AUTOMATISK", "Automatisk generert vedtaksbrev"),
    @Deprecated
    FRITEKST("FRITEKST", "Fritekstbrev"),
    MANUELL("MANUELL", "Manuell vedtaksbrev"),
    INGEN("INGEN", "Ingen vedtaksbrev"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, Vedtaksbrev> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VEDTAKSBREV";

    @JsonIgnore
    private String navn;

    private String kode;

    private Vedtaksbrev(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static Vedtaksbrev fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Vedtaksbrev.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Vedtaksbrev: for input " + node);
        }
        return ad;
    }

    public static Map<String, Vedtaksbrev> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
