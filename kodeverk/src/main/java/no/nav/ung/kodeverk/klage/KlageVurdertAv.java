package no.nav.ung.kodeverk.klage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum KlageVurdertAv implements Kodeverdi {

    NFP("NFP", "NAV Familie- og Pensjonsytelser"),
    NAY("NAY", "NAV Arbeid og ytelser"),
    NK("NK", "NAV Klageenhet K9"),
    NK_KABAL("NKK", "NAV Klageenhet Kabal"),
    UDEFINERT("Udefinert", "Udenfinert");

    private static final Map<String, KlageVurdertAv> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_VURDERT_AV";

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

    private KlageVurdertAv(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static KlageVurdertAv fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageVurdertAv: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageVurdertAv> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
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

    public boolean erKlageenhet() {
        return this == NK || this == NK_KABAL;
    }
}
