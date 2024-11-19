package no.nav.ung.kodeverk.dokument;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

public enum DokumentMalType implements Kodeverdi {
    // FIXME K9 Rydd herifra
    INNHENT_DOK("INNHEN", "Innhent dokumentasjon"),
    HENLEGG_BEHANDLING_DOK("HENLEG", "Behandling henlagt"),
    UENDRETUTFALL_DOK("UENDRE", "Uendret utfall"),
    FORLENGET_DOK("FORLEN", "Forlenget saksbehandlingstid"),
    FORLENGET_MEDL_DOK("FORLME", "Forlenget saksbehandlingstid - medlemskap"),
    FORLENGET_TIDLIG_SOK("FORLTS", "Forlenget saksbehandlingstid - Tidlig søknad"),
    FORLENGET_OPPTJENING("FOROPT", "Forlenget saksbehandlingstid - Venter Opptjening"),
    REVURDERING_DOK("REVURD", "Varsel om revurdering"),
    INNVILGELSE_DOK("INNVILGELSE", "Innvilgelsesbrev"),
    OPPHØR_DOK("OPPHOR", "Opphør brev"),
    INNTEKTSMELDING_FOR_TIDLIG_DOK("INNTID", "Ikke mottatt søknad"),
    AVSLAG__DOK("AVSLAG", "Avslagsbrev"),
    FRITEKST_DOK("FRITKS", "Fritekstbrev"),
    ETTERLYS_INNTEKTSMELDING_DOK("INNLYS", "Etterlys inntektsmelding"),
    ETTERLYS_INNTEKTSMELDING_PURRING("INNLYS_PURRING", "Etterlys inntektsmelding - purring"),
    GENERELT_FRITEKSTBREV("GENERELT_FRITEKSTBREV", "Fritekst generelt brev"),
    VARSEL_FRITEKST("VARSEL_FRITEKST", "Varselsbrev fritekst"),

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

    private String kode;

    private DokumentMalType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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

    @JsonCreator(mode = Mode.DELEGATING)
    public static DokumentMalType  fraKode(Object node)  {
        if (node == null) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + node);
        }
        String kode = TempAvledeKode.getVerdi(DokumentMalType.class, node, "kode");
        var ad = Optional.ofNullable(KODER.get(kode));
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent DokumentMalType: " + kode);
        }
        return ad.get();
    }

}
