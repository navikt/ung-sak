package no.nav.ung.kodeverk.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.time.Period;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    private String navn;

    private String kode;
    private String offisiellKode;
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

    public static InntektPeriodeType fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
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
