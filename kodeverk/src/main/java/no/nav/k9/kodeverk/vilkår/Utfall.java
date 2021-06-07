package no.nav.k9.kodeverk.vilkår;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
public enum Utfall implements Kodeverdi {
    IKKE_OPPFYLT("IKKE_OPPFYLT", "Ikke oppfylt", 0),
    IKKE_VURDERT("IKKE_VURDERT", "Ikke vurdert", 1),
    OPPFYLT("OPPFYLT", "Oppfylt", 2),
    IKKE_RELEVANT("IKKE_RELEVANT", "Ikke relevant", 4),
    UDEFINERT("-", "Ikke definert", Integer.MAX_VALUE),
    ;

    public static final String KODEVERK = "VILKAR_UTFALL_TYPE";
    private static final Map<String, Utfall> KODER = new LinkedHashMap<>();

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
    private int rank;

    private Utfall(String kode) {
        this.kode = kode;
    }

    private Utfall(String kode, String navn, int rank) {
        this.kode = kode;
        this.navn = navn;
        this.rank = rank;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static Utfall fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Utfall.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Utfall: for input " + node);
        }
        return ad;
    }

    public static Map<String, Utfall> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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

    int getRank() {
        return rank;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public static Utfall ranger(Collection<Utfall> utfall) {
        if (utfall == null || utfall.isEmpty()) {
            return null;
        }
        return utfall.stream()
            .filter(u -> u != UDEFINERT)
            .sorted(Comparator.comparing(Utfall::getRank))
            .findFirst().orElse(null);
    }
}
