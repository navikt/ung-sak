package no.nav.k9.kodeverk.risikoklassifisering;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import no.nav.k9.kodeverk.api.Kodeverdi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Kontrollresultat implements Kodeverdi {

    HØY("HOY", "Kontrollresultatet er HØY"),
    IKKE_HØY("IKKE_HOY", "Kontrollresultatet er IKKE_HØY"),
    IKKE_KLASSIFISERT("IKKE_KLASSIFISERT", "Behandlingen er ikke blitt klassifisert"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, Kontrollresultat> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KONTROLLRESULTAT";

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

    private Kontrollresultat(String kode) {
        this.kode = kode;
    }

    private Kontrollresultat(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static Kontrollresultat fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Kontrollresultat: " + kode);
        }
        return ad;
    }

    public static Map<String, Kontrollresultat> kodeMap() {
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

}
