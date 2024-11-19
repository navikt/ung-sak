package no.nav.ung.kodeverk.opptjening;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum OpptjeningAktivitetKlassifisering implements Kodeverdi {

    BEKREFTET_GODKJENT("BEKREFTET_GODKJENT", "Bekreftet godkjent"),
    BEKREFTET_AVVIST("BEKREFTET_AVVIST", "Bekreftet avvist"),
    ANTATT_GODKJENT("ANTATT_GODKJENT", "Antatt godkjent"),
    MELLOMLIGGENDE_PERIODE("MELLOMLIGGENDE_PERIODE", "Mellomliggende periode"),
    UDEFINERT("-", "UDEFINERT"),
    ;

    private static final Map<String, OpptjeningAktivitetKlassifisering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPTJENING_AKTIVITET_KLASSIFISERING";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;

    private OpptjeningAktivitetKlassifisering(String kode) {
        this.kode = kode;
    }

    private OpptjeningAktivitetKlassifisering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static OpptjeningAktivitetKlassifisering  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(OpptjeningAktivitetKlassifisering.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OpptjeningAktivitetKlassifisering: " + kode);
        }
        return ad;
    }

    public static Map<String, OpptjeningAktivitetKlassifisering> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    /**
     * toString is set to output the kode value of the enum instead of the default that is the enum name.
     * This makes the generated openapi spec correct when the enum is used as a query param. Without this the generated
     * spec incorrectly specifies that it is the enum name string that should be used as input.
     */
    @Override
    public String toString() {
        return this.getKode();
    }

    @Override
    public String getNavn() {
        return navn;
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
