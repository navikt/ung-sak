package no.nav.k9.sak.behandlingslager.hendelser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum StartpunktType implements Kodeverdi {

    INIT_PERIODER("INIT_PERIODER", "Initier perioder", 1),
    KONTROLLER_ARBEIDSFORHOLD("KONTROLLER_ARBEIDSFORHOLD", "Startpunkt kontroller arbeidsforhold", 2),
    KONTROLLER_FAKTA("KONTROLLER_FAKTA", "Kontroller fakta", 3),
    INNGANGSVILKÅR_OPPLYSNINGSPLIKT("INNGANGSVILKÅR_OPPL", "Inngangsvilkår opplysningsplikt", 4),
    INNGANGSVILKÅR_OMSORGENFOR("INNGANGSVILKÅR_OMSORGENFOR", "Inngangsvilkår omsorgen for", 10),
    INNGANGSVILKÅR_MEDISINSK("INNGANGSVILKÅR_MEDISINSK", "Inngangsvilkår sykdom", 12),
    INNGANGSVILKÅR_MEDLEMSKAP("INNGANGSVILKÅR_MEDL", "Inngangsvilkår medlemskapsvilkår", 15),
    OPPTJENING("OPPTJENING", "Opptjening", 20),
    BEREGNING("BEREGNING", "Beregning", 25),
    UTTAKSVILKÅR("UTTAKSVILKÅR", "Uttaksvilkår", 30),

    UDEFINERT("-", "Ikke definert", 99),
    ;

    public static final String KODEVERK = "STARTPUNKT_TYPE";
    private static final Map<String, StartpunktType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private int rangering;

    @JsonIgnore
    private String navn;

    private String kode;


    StartpunktType(String kode, String navn, int rangering) {
        this.kode = kode;
        this.navn = navn;
        this.rangering = rangering;
    }

    @JsonCreator
    public static StartpunktType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent StartpunktType: " + kode);
        }
        return ad;
    }

    public static Map<String, StartpunktType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return this.kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public int getRangering() {
        return rangering;
    }
}
