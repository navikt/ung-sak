package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum VurderÅrsak implements Kodeverdi {

    FEIL_FAKTA("FEIL_FAKTA", "Feil fakta"),
    FEIL_LOV("FEIL_LOV", "Feil lovanvendelse"),
    FEIL_REGEL("FEIL_REGEL", "Feil regelforståelse"),
    ANNET("ANNET", "Annet"),
    UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VurderÅrsak> KODER = new LinkedHashMap<>();
    public static final String KODEVERK = "VURDER_AARSAK";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private VurderÅrsak() {
    }

    private VurderÅrsak(String kode) {
        this.kode = kode;
    }

    private VurderÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static VurderÅrsak fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VurderÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, VurderÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Override
    public String getNavn() {
        return navn;
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

}
