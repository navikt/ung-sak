package no.nav.ung.kodeverk.person;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.TempAvledeKode;
import no.nav.ung.kodeverk.api.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
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

    @JsonIgnore
    private String navn;

    private String kode;

    private RelasjonsRolleType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
    public static RelasjonsRolleType  fraKode(Object node)  {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(RelasjonsRolleType.class, node, "kode");
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
