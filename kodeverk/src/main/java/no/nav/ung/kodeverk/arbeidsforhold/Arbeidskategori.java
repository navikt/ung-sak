package no.nav.ung.kodeverk.arbeidsforhold;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum Arbeidskategori implements Kodeverdi {

    FISKER("FISKER", "Selvstendig næringsdrivende - Fisker"),
    ARBEIDSTAKER("ARBEIDSTAKER", "Arbeidstaker"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    KOMBINASJON_ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE("KOMBINASJON_ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE",
            "Kombinasjon arbeidstaker og selvstendig næringsdrivende"),
    SJØMANN("SJØMANN", "Arbeidstaker - sjømann"),
    JORDBRUKER("JORDBRUKER", "Selvstendig næringsdrivende - Jordbruker"),
    DAGPENGER("DAGPENGER", "Tilstøtende ytelse - dagpenger"),
    INAKTIV("INAKTIV", "Inaktiv"),
    KOMBINASJON_ARBEIDSTAKER_OG_JORDBRUKER("KOMBINASJON_ARBEIDSTAKER_OG_JORDBRUKER", "Kombinasjon arbeidstaker og selvstendig næringsdrivende - jordbruker"),
    KOMBINASJON_ARBEIDSTAKER_OG_FISKER("KOMBINASJON_ARBEIDSTAKER_OG_FISKER", "Kombinasjon arbeidstaker og selvstendig næringsdrivende - fisker"),
    FRILANSER("FRILANSER", "Frilanser"),
    KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER("KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER", "Kombinasjon arbeidstaker og frilanser"),
    KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER("KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER", "Kombinasjon arbeidstaker og dagpenger"),
    DAGMAMMA("DAGMAMMA", "Selvstendig næringsdrivende - Dagmamma"),
    UGYLDIG("UGYLDIG", "Ugyldig"),
    UDEFINERT("-", "Ingen inntektskategori (default)"),
    ;

    private static final Map<String, Arbeidskategori> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ARBEIDSKATEGORI";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private Arbeidskategori(String kode) {
        this.kode = kode;
    }

    private Arbeidskategori(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Arbeidskategori fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Arbeidskategori: " + kode);
        }
        return ad;
    }

    public static Map<String, Arbeidskategori> kodeMap() {
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

}
