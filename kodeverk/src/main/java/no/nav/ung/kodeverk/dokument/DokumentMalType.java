package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Brevkoder for brev som kan bestilles. Brukes blant annet til journalføring.
 */
public enum DokumentMalType implements Kodeverdi {
    HENLEGG_BEHANDLING_DOK("HENLEG", "Behandling henlagt", false),

//    UENDRETUTFALL_DOK("UENDRE", "Uendret utfall", true),
//    FORLENGET_DOK("FORLEN", "Forlenget saksbehandlingstid", false),
    INNVILGELSE_DOK("INNVILGELSE", "Innvilgelsesbrev", true),
    ENDRING_BARNETILLEGG("ENDRING_BARNETILLEGG", "Endring barnetillegg", true),
    ENDRING_PROGRAMPERIODE("ENDRING_PROGRAMPERIODE", "Endring programperiode", true),
    ENDRING_INNTEKT("ENDRING_INNTEKT", "Endring inntekt", true),
    ENDRING_HØY_SATS("ENDRING_HØY_SATS", "Endring høy sats", true),
    OPPHØR_DOK("OPPHOR", "Opphør brev", true),
    AVSLAG__DOK("AVSLAG", "Avslagsbrev", true),
    MANUELT_VEDTAK_DOK("MANUELL", "Fritekst vedtaksbrev", true),
    GENERELT_FRITEKSTBREV("GENERELT_FRITEKSTBREV", "Fritekst generelt brev", false);
//    VARSEL_FRITEKST("VARSEL_FRITEKST", "Varselsbrev fritekst", false);


    private static final Map<String, DokumentMalType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;
    private final boolean vedtaksbrevmal;
    private final String kode;

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

    public boolean isVedtaksbrevmal() {
        return vedtaksbrevmal;
    }

    public static DokumentMalType fraKode(final String kode) {
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + kode);
        }
        return ad.get();
    }

}
