package no.nav.ung.kodeverk.person;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RelasjonsRolleType implements Kodeverdi {

    EKTE("EKTE", "Ektefelle til"),
    BARN("BARN", "Barn av"),
    FARA("FARA", "Far til"),
    MORA("MORA", "Mor til"),
    REGISTRERT_PARTNER("REPA", "Registrert partner med"),
    SAMBOER("SAMB", "Samboer med"),
    MEDMOR("MMOR", "Medmor"),

    ANNEN_PART_FRA_SØKNAD("ANPA", "Annen part fra søknad"),

    // TODO: sjekk denne
    @Deprecated
    BARN_FRA_SØKNAD("BASO", "Barn fra søknad"),

    // TODO: sjekk denne
    @Deprecated
    HOVEDSØKER_FRA_SØKNAD("HOVS", "Hovedsøker fra søknad"),

    FOSTERBARN("K9_FOSTERBARN", "Fosterbarn"),
    FOSTERFORELDER("K9_FOSTERFORELDER", "Fosterforelder"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, RelasjonsRolleType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "RELASJONSROLLE_TYPE";

    private static final Set<RelasjonsRolleType> FORELDRE_ROLLER = Stream.of(RelasjonsRolleType.MORA, RelasjonsRolleType.FARA, RelasjonsRolleType.MEDMOR)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    private RelasjonsRolleType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static RelasjonsRolleType  fraKode(final String kode)  {
        if (kode == null) {
            return null;
        }
        var ad = fraKodeOptional(kode);
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent RelasjonsRolleType: " + kode);
        }
        return ad.get();
    }

    public static Map<String, RelasjonsRolleType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static Optional<RelasjonsRolleType> fraKodeOptional(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(KODER.get(kode));
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

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public static boolean erFar(RelasjonsRolleType relasjon) {
        return FARA.getKode().equals(relasjon.getKode());
    }

    public static boolean erMedmor(RelasjonsRolleType relasjon) {
        return MEDMOR.getKode().equals(relasjon.getKode());
    }

    public static boolean erFarEllerMedmor(RelasjonsRolleType relasjon) {
        return erFar(relasjon) || erMedmor(relasjon);
    }

    public static boolean erMor(RelasjonsRolleType relasjon) {
        return MORA.getKode().equals(relasjon.getKode());
    }

    public static boolean erRegistrertForeldre(RelasjonsRolleType type) {
        return FORELDRE_ROLLER.contains(type);
    }

}
