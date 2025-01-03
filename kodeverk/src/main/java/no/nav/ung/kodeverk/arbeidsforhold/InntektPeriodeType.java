package no.nav.ung.kodeverk.arbeidsforhold;

import java.time.Period;
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
public enum InntektPeriodeType implements Kodeverdi {

    DAGLIG("DAGLG", "Daglig", "D", Period.ofDays(1)),
    UKENTLIG("UKNLG", "Ukentlig", "U", Period.ofWeeks(1)),
    BIUKENTLIG("14DLG", "Fjorten-daglig", "F", Period.ofWeeks(2)),
    MÅNEDLIG("MNDLG", "Månedlig", "M", Period.ofMonths(1)),
    ÅRLIG("AARLG", "Årlig", "Å", Period.ofYears(1)),
    FASTSATT25PAVVIK("INNFS", "Fastsatt etter 25 prosent avvik", "X", Period.ofYears(1)),
    PREMIEGRUNNLAG("PREMGR", "Premiegrunnlag", "Y", Period.ofYears(1)),
    UDEFINERT("-", "Ikke definert", null, null),
    ;

    private static final Map<String, InntektPeriodeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "INNTEKT_PERIODE_TYPE";

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
    @JsonIgnore
    private String offisiellKode;
    @JsonIgnore
    private Period periode;

    private InntektPeriodeType(String kode) {
        this.kode = kode;
    }

    private InntektPeriodeType(String kode, String navn, String offisiellKode, Period periode) {
        this.kode = kode;
        this.navn = navn;
        this.periode = periode;
        this.offisiellKode = offisiellKode;
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

    @JsonCreator(mode = Mode.DELEGATING)
    public static InntektPeriodeType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(InntektPeriodeType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent InntektPeriodeType: " + kode);
        }
        return ad;
    }

    public static Map<String, InntektPeriodeType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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

    public Period getPeriode() {
        return periode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
