package no.nav.ung.kodeverk.vilkår;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

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

    private String navn;

    private String kode;

    private int rank;

    private Utfall(String kode) {
        this.kode = kode;
    }

    private Utfall(String kode, String navn, int rank) {
        this.kode = kode;
        this.navn = navn;
        this.rank = rank;
    }

    public static Utfall fraKode(final String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Utfall: for input " + kode);
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonValue
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
