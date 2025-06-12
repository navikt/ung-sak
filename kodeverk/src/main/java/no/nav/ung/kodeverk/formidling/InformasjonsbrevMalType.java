package no.nav.ung.kodeverk.formidling;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.dokument.DokumentMalType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Informasjon og støttebrev som kan bestilles.
 */
public enum InformasjonsbrevMalType implements Kodeverdi {

    GENERELT_FRITEKSTBREV(DokumentMalType.GENERELT_FRITEKSTBREV)

    ;
    private static final Map<String, InformasjonsbrevMalType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.getKode(), v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.getKode());
            }
        }
    }

    private final DokumentMalType dokumentMalType;

    InformasjonsbrevMalType(DokumentMalType dokumentMalType) {
        this.dokumentMalType = dokumentMalType;
    }

    @JsonValue
    @Override
    public String getKode() {
        return dokumentMalType.getKode();
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getKodeverk() {
        return "INFORMASJONSBREV_MAL_TYPE";
    }

    @Override
    public String getNavn() {
        return dokumentMalType.getNavn();
    }

    public static InformasjonsbrevMalType fraKode(final String kode) {
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent InformasjonsbrevMalType: " + kode);
        }
        return ad.get();
    }

}
