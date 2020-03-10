package no.nav.k9.kodeverk.uttak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum UttakArbeidType implements Kodeverdi {

    ARBEIDSTAKER(Inntektskategori.ARBEIDSTAKER.getKode(), "Ordinært arbeid"),
    SELVSTENDIG_NÆRINGSDRIVENDE(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE.getKode(), "Selvstendig næringsdrivende"),
    FRILANS(Inntektskategori.FRILANSER.getKode(), "Frilans"),
    
    ANNET(Inntektskategori.UDEFINERT.name(), "Annet"),
    ;

    private static final Map<String, UttakArbeidType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTTAK_ARBEID_TYPE";

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

    UttakArbeidType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static UttakArbeidType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UttakArbeidType: " + kode);
        }
        return ad;
    }

    public static Map<String, UttakArbeidType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
        return this.getKode();
    }

    public boolean erArbeidstakerEllerFrilans() {
        return ARBEIDSTAKER.equals(this) || FRILANS.equals(this);
    }
}
