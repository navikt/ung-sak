package no.nav.ung.kodeverk.dokument;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum DokumentMalRestriksjon implements Kodeverdi {

    INGEN("INGEN", "ingen"),
    REVURDERING("REVURDERING", "Mal for revurdering"),
    ÅPEN_BEHANDLING("ÅPEN_BEHANDLING", "Brev kan bare sendes fra en åpen behandling"),
    ÅPEN_BEHANDLING_IKKE_SENDT("ÅPEN_BEHANDLING_IKKE_SENDT", "Brev kan bare sendes en gang"),
    ;

    private static final Map<String, DokumentMalRestriksjon> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "DOKUMENT_MAL_RESTRIKSJON";

    @Deprecated
    public static final String DISCRIMINATOR = "DOKUMENT_MAL_RESTRIKSJON";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private DokumentMalRestriksjon(String kode) {
        this.kode = kode;
    }

    private DokumentMalRestriksjon(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static DokumentMalRestriksjon  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent DokumentMalRestriksjon: " + kode);
        }
        return ad;
    }

    public static Map<String, DokumentMalRestriksjon> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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
        return getKode();
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
}
