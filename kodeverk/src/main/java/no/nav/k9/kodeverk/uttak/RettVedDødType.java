package no.nav.k9.kodeverk.uttak;

import com.fasterxml.jackson.annotation.*;
import no.nav.k9.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum RettVedDødType implements Kodeverdi {

    RETT_6_UKER("RETT_6_UKER", "Rett til 6 uker"),
    RETT_12_UKER("RETT_12_UKER", "Rett til 12 uker");

    private static final Map<String, RettVedDødType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "RETT_VED_DØD_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
            KODER.putIfAbsent(v.name(), v); // kompatibilitet
        }
    }


    @JsonValue
    private String kode;

    @JsonIgnore
    private String navn;


    private RettVedDødType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static RettVedDødType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UtfallType: " + kode);
        }
        return ad;
    }

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

    @Override
    public String getNavn() {
        return navn;
    }
}
