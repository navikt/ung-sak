package no.nav.ung.kodeverk.beregningsgrunnlag.kompletthet;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum Vurdering implements Kodeverdi {

    UDEFINERT("-", "Udefinert"),
    KAN_FORTSETTE("FORTSETT", "Kan beregnes på bakgrunn av opplysninger fra a-ordningen"),
    MANGLENDE_GRUNNLAG("MANGLENDE_GRUNNLAG", "Kan ikke forsett pga manglende grunnlag for å kunne beregne."),
    UAVKLART("UAVKLART", "Uavklart");

    private static final Map<String, Vurdering> KODER = new LinkedHashMap<>();
    public static final String KODEVERK = "KOMPLETTHET_VURDERING";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;
    private String beskrivelse;

    Vurdering(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    public static Vurdering fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Venteårsak: " + kode);
        }
        return ad;
    }

    public static Map<String, Vurdering> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonValue
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return getKode();
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
