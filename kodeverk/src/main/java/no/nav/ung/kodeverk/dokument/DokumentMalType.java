package no.nav.ung.kodeverk.dokument;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.api.Kodeverdi;

public enum DokumentMalType implements Kodeverdi {
    HENLEGG_BEHANDLING_DOK("HENLEG", "Behandling henlagt", false),

//    UENDRETUTFALL_DOK("UENDRE", "Uendret utfall", true),
//    FORLENGET_DOK("FORLEN", "Forlenget saksbehandlingstid", false),
    INNVILGELSE_DOK("INNVILGELSE", "Innvilgelsesbrev", true),
    ENDRING_DOK("ENDRING", "Endring vedtaksbrev", true),
    OPPHØR_DOK("OPPHOR", "Opphør brev", true),
    AVSLAG__DOK("AVSLAG", "Avslagsbrev", true),
    MANUELT_VEDTAK_DOK("MANUELL", "Fritekst vedtaksbrev", true),
//    GENERELT_FRITEKSTBREV("GENERELT_FRITEKSTBREV", "Fritekst generelt brev", false),
//    VARSEL_FRITEKST("VARSEL_FRITEKST", "Varselsbrev fritekst", false),

    ;

    private static final Map<String, DokumentMalType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private final boolean vedtaksbrevmal;

    private String kode;



    DokumentMalType(String kode, String navn, boolean vedtaksbrevmal) {
        this.kode = kode;
        this.navn = navn;
        this.vedtaksbrevmal = vedtaksbrevmal;
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return "DOKUMENT_MAL_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonCreator
    public static DokumentMalType fraKode(String kode) {
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + kode);
        }
        return ad.get();
    }

}
