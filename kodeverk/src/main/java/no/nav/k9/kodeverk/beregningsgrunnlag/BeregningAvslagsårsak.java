package no.nav.k9.kodeverk.beregningsgrunnlag;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum BeregningAvslagsårsak implements Kodeverdi {

    SØKT_FL_INGEN_FL_INNTEKT("SØKT_FL_INGEN_FL_INNTEKT", "Søkt frilans uten frilansinntekt"),
    FOR_LAVT_BG("FOR_LAVT_BG", "For lavt beregningsgrunnlag"),

    UNDEFINED,;

    static final String KODEVERK = "BEREGNING_AVSLAG_ÅRSAK";

    private static final Map<String, BeregningAvslagsårsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;

    @JsonIgnore
    private String navn;

    private BeregningAvslagsårsak() {
        // for hibernate
    }

    private BeregningAvslagsårsak(String kode, String navn) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    @JsonCreator
    public static BeregningAvslagsårsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningAvslagsårsak: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningAvslagsårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

}
