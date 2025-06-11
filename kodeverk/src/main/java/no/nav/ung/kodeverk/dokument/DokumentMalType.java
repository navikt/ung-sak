package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

    private String navn;

    private final boolean vedtaksbrevmal;

    private String kode;



    DokumentMalType(String kode, String navn, boolean vedtaksbrevmal) {
        this.kode = kode;
        this.navn = navn;
        this.vedtaksbrevmal = vedtaksbrevmal;
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
        return "DOKUMENT_MAL_TYPE";
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static DokumentMalType fraKode(final String kode) {
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + kode);
        }
        return ad.get();
    }

}
