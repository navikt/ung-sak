package no.nav.ung.kodeverk.beregningsgrunnlag;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum PeriodeÅrsak implements Kodeverdi {

    NATURALYTELSE_BORTFALT("NATURALYTELSE_BORTFALT", "Naturalytelse bortfalt"),
    ARBEIDSFORHOLD_AVSLUTTET("ARBEIDSFORHOLD_AVSLUTTET", "Arbeidsforhold avsluttet"),
    NATURALYTELSE_TILKOMMER("NATURALYTELSE_TILKOMMER", "Naturalytelse tilkommer"),
    ENDRING_I_REFUSJONSKRAV("ENDRING_I_REFUSJONSKRAV", "Endring i refusjonskrav"),
    REFUSJON_OPPHØRER("REFUSJON_OPPHØRER", "Refusjon opphører"),
    GRADERING("GRADERING", "Gradering"),
    GRADERING_OPPHØRER("GRADERING_OPPHØRER", "Gradering opphører"),
    ENDRING_I_AKTIVITETER_SØKT_FOR("ENDRING_I_AKTIVITETER_SØKT_FOR", "Endring i aktiviteter søkt for"),
    REFUSJON_AVSLÅTT("REFUSJON_AVSLÅTT", "Vilkåret for refusjonskravfrist er avslått"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, PeriodeÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERIODE_AARSAK";

    @Deprecated
    public static final String DISCRIMINATOR = "PERIODE_AARSAK";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private PeriodeÅrsak(String kode) {
        this.kode = kode;
    }

    private PeriodeÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static PeriodeÅrsak fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PeriodeÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, PeriodeÅrsak> kodeMap() {
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

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
