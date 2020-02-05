package no.nav.k9.kodeverk.arbeidsforhold;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.api.Kodeverdi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Inntektskategori implements Kodeverdi {

    ARBEIDSTAKER("ARBEIDSTAKER", "Arbeidstaker"),
    FRILANSER("FRILANSER", "Frilans"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    DAGPENGER("DAGPENGER", "Dagpenger"),
    ARBEIDSAVKLARINGSPENGER("ARBEIDSAVKLARINGSPENGER", "Arbeidsavklaringspenger"),
    SJØMANN("SJØMANN", "Arbeidstaker - Sjømann"),
    DAGMAMMA("DAGMAMMA", "Selvstendig næringsdrivende (dagmamma)"),
    JORDBRUKER("JORDBRUKER", "Selvstendig næringsdrivende - Jordbruker"),
    FISKER("FISKER", "Selvstendig næringsdrivende (fisker)"),
    ARBEIDSTAKER_UTEN_FERIEPENGER("ARBEIDSTAKER_UTEN_FERIEPENGER", "Arbeidstaker uten feriepenger"),
    UDEFINERT("-", "Ingen inntektskategori (default)"),
    ;
    private static final Map<String, Inntektskategori> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "INNTEKTSKATEGORI";

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

    Inntektskategori(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static Inntektskategori fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Inntektskategori: " + kode);
        }
        return ad;
    }
    public static Map<String, Inntektskategori> kodeMap() {
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
    
    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(k -> "'" + k + "'").collect(Collectors.toList()));
    }
}
