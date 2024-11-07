package no.nav.k9.kodeverk.uttak;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum SøknadÅrsak implements Kodeverdi {
    ARBEIDSGIVER_KONKURS("ARBEIDSGIVER_KONKURS", "Arbeidsgiver er konkurs"),
    NYOPPSTARTET_HOS_ARBEIDSGIVER("NYOPPSTARTET_HOS_ARBEIDSGIVER", "Har startet hos ny arbeidsgiver"),
    KONFLIKT_MED_ARBEIDSGIVER("KONFLIKT_MED_ARBEIDSGIVER", "Konflikt med arbeidsgiver"),
    UDEFINERT("-", "Udefinert");

    private String kode;
    private String navn;

    public static final String KODEVERK = "SØKNAD_ÅRSAK";

    private static final Map<String, SøknadÅrsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            KODER.putIfAbsent(v.name(), v); // fallback for Jackson enum key i map issue (løses delvis i jackson 2.11)
        }
    }

    private SøknadÅrsak(String kode, String navn) {
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

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return this.getKode();
    }

    @JsonCreator
    public static SøknadÅrsak fraKode(String kode) {
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
