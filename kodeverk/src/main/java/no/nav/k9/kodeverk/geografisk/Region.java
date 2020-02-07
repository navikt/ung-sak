package no.nav.k9.kodeverk.geografisk;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
public enum Region implements Kodeverdi {

    NORDEN("NORDEN", "Norden",
            Set.of("ALA",
                "DNK",
                "FIN", "FRO",
                "GRL",
                "ISL",
                "NOR",
                "SWE")),
    EOS("EOS", "EØS",
            Set.of("ALA", "AUT",
                "BEL", "BGR",
                "CYP", "CZE",
                "DEU", "DNK",
                "ESP", "EST",
                "FIN", "FRA", "FRO",
                "GBR", "GRC", "GRL",
                "HRV", "HUN",
                "IRL", "ISL", "ITA",
                "LIE", "LTU", "LUX", "LVA",
                "MLT",
                "NLD", "NOR",
                "POL", "PRT",
                "ROU",
                "SVK", "SVN", "SWE")),
    TREDJELANDS_BORGER("ANNET", "3djelandsborger", landUtenom(EOS, NORDEN)),
    UDEFINERT("-", "Ikke definert", Set.of()),
    ;

    public static final String KODEVERK = "REGION"; //$NON-NLS-1$
    public static final String DISCRIMINATOR = "REGION";
    private static final Map<String, Region> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String kode;

    @JsonIgnore
    private Set<String> land;

    private Region(String kode) {
        this.kode = kode;
    }

    private static Set<String> landUtenom(Region... unntattRegioner) {
        Set<String> land = new LinkedHashSet<>(Landkoder.kodeMap().keySet());
        for (var r : unntattRegioner) {
            land.removeAll(r.getLand());
        }
        return Collections.unmodifiableSet(land);
    }

    private Region(String kode, String navn, Set<String> land) {
        this.kode = kode;
        this.navn = navn;
        this.land = land;
    }

    @JsonCreator
    public static Region fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakYtelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, Region> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public Set<String> getLand() {
        return land;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public static List<Region> fraLandkode(String landKode) {
        return KODER.values().stream()
            .filter(v -> v.getLand().contains(landKode))
            .collect(Collectors.toList());
    }
    
    public static Region finnHøyestRangertRegion(List<String> statsborgerskap) {
        Set<Region> regioner = new HashSet<>();
        for (String skap : statsborgerskap) {
            regioner.addAll(finnRegioner(skap));
        }
        return regioner.stream().min(Comparator.comparing(Region::rangerRegion)).get();
    }

    public static Map<Landkoder, Region> finnRegionForStatsborgerskap(List<Landkoder> statsborgerskap) {
        final HashMap<Landkoder, Region> landRegion = new HashMap<>();
        for (Landkoder landkode : statsborgerskap) {
            landRegion.put(landkode, finnRegioner(landkode.getKode()).stream().min(Comparator.comparing(Region::rangerRegion)).orElse(Region.TREDJELANDS_BORGER));
        }
        return landRegion;
    }

    // Det finnes ingen definert rangering for regioner. Men venter med å generalisere til det finnes use-caser som
    // krever en annen rangering enn nedenfor.
    private static Integer rangerRegion(Region region) {
        if (region.equals(Region.NORDEN)) {
            return 1;
        }
        if (region.equals(Region.EOS)) {
            return 2;
        }
        return 3;
    }

    public static List<Region> finnRegioner(String landKode) {
        List<Region> regionKoder = Region.fraLandkode(landKode);
        
        if (regionKoder.isEmpty()) {
            return List.of(Region.TREDJELANDS_BORGER);
        }

        return regionKoder;
    }


}
