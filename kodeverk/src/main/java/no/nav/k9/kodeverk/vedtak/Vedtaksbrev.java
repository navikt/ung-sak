package no.nav.k9.kodeverk.vedtak;

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

import no.nav.k9.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Vedtaksbrev implements Kodeverdi{

    AUTOMATISK("AUTOMATISK", "Automatisk generert vedtaksbrev"),
    FRITEKST("FRITEKST", "Fritekstbrev"),
    INGEN("INGEN", "Ingen vedtaksbrev"),
    UDEFINERT("-", "Udefinert"),
    ;
    
    private static final Map<String, Vedtaksbrev> KODER = new LinkedHashMap<>();
    
    
    public static final String KODEVERK = "VEDTAKSBREV";
    
    @JsonIgnore
    private String navn;

    private String kode;

    private Vedtaksbrev(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }
    
    @JsonCreator
    public static Vedtaksbrev fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Vedtaksbrev: " + kode);
        }
        return ad;
    }

    public static Map<String, Vedtaksbrev> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
    
    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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
        return getKode();
    }
    
    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


}
