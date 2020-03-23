package no.nav.k9.sak.behandlingslager.behandling.opptjening;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum ReferanseType {

    ORG_NR("ORG_NR", "Orgnr"),
    AKTØR_ID("AKTØR_ID", "Aktør Id"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, ReferanseType> KODER = new LinkedHashMap<>();

    @Deprecated
    public static final String DISCRIMINATOR = "REFERANSE_TYPE";

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

    private ReferanseType(String kode) {
        this.kode = kode;
    }

    private ReferanseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static ReferanseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ReferanseType: " + kode);
        }
        return ad;
    }

    public static Map<String, ReferanseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonProperty
    public String getKode() {
        return kode;
    }

}
