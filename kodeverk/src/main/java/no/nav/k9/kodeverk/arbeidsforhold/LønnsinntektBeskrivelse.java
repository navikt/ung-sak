package no.nav.k9.kodeverk.arbeidsforhold;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.TempAvledeKode;
import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum LønnsinntektBeskrivelse implements Kodeverdi {

    KOMMUNAL_OMSORGSLOENN_OG_FOSTERHJEMSGODTGJOERELSE("KOMMUNAL_OMSORGSLOENN_OG_FOSTERHJEMSGODTGJOERELSE", "Kommunal omsorgslønn og fosterhjemsgodtgjørelse", "kommunalOmsorgsloennOgFosterhjemsgodtgjoerelse"),
    UDEFINERT("-", "Udefinert", "Ikke definert"),
    ;

    private static final Map<String, LønnsinntektBeskrivelse> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "LONNSINNTEKT_BESKRIVELSE";

    @Deprecated
    public static final String DISCRIMINATOR = "LONNSINNTEKT_BESKRIVELSE";

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
    @JsonIgnore
    private String offisiellKode;

    private LønnsinntektBeskrivelse(String kode) {
        this.kode = kode;
    }

    private LønnsinntektBeskrivelse(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    /**
     * toString is set to output the kode value of the enum instead of the default that is the enum name.
     * This makes the generated openapi spec correct when the enum is used as a query param. Without this the generated
     * spec incorrectly specifies that it is the enum name string that should be used as input.
     */
    @Override
    public String toString() {
        return this.getKode();
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static LønnsinntektBeskrivelse fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(LønnsinntektBeskrivelse.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent LønnsinntektBeskrivelse: " + kode);
        }
        return ad;
    }

    public static Map<String, LønnsinntektBeskrivelse> kodeMap() {
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
        return offisiellKode;
    }

}
