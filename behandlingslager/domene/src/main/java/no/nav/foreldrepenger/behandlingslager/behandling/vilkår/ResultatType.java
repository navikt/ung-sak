package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

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
public enum ResultatType {
     INNVILGET("INNVILGET", "Innvilget"),
     DELEVIS_AVSLÅTT("DELEVIS_AVSLÅTT", "Delevis avslått"),
     AVSLÅTT("AVSLAATT", "Avslått"),
     IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
     UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, ResultatType> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;

    private String kode;

    private ResultatType(String kode) {
        this.kode = kode;
    }

    private ResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static ResultatType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårResultatType: " + kode);
        }
        return ad;
    }

    public static Map<String, ResultatType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonProperty
    public String getKode() {
        return kode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
