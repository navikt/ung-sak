package no.nav.ung.kodeverk.produksjonsstyring;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

public enum UtvidetSøknadÅrsak implements Kodeverdi {
    ARBEIDSGIVER_KONKURS("ARBEIDSGIVER_KONKURS", "Arbeidsgiver er konkurs"),
    NYOPPSTARTET_HOS_ARBEIDSGIVER("NYOPPSTARTET_HOS_ARBEIDSGIVER", "Har startet hos ny arbeidsgiver"),
    KONFLIKT_MED_ARBEIDSGIVER("KONFLIKT_MED_ARBEIDSGIVER", "Konflikt med arbeidsgiver"),
    SN("SN", "Selvstendig næringsdrivende"),
    FL("FL", "Frilanser");

    private String kode;
    private String navn;

    public static final String KODEVERK = "UTVIDET_SØKNAD_ÅRSAK";

    private static final Map<String, UtvidetSøknadÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            KODER.putIfAbsent(v.name(), v); // fallback for Jackson enum key i map issue (løses delvis i jackson 2.11)
        }
    }

    private UtvidetSøknadÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
        return this.getKode();
    }

    public static UtvidetSøknadÅrsak fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SøknadÅrsak: " + kode);
        }
        return ad;
    }

}
