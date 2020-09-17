package no.nav.k9.kodeverk.vilkår;

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
public enum VilkårUtfallMerknad implements Kodeverdi {

    VM_1001("1001", "Søknad er sendt før 26. svangerskapsuke er passert og barnet er ikke født"),
    VM_1002("1002", "Søker er medmor (forelder2) og har søkt om engangsstønad til mor"),
    VM_1003("1003", "Søker er far og har søkt om engangsstønad til mor"),
    VM_1004("1004", "Barn over 15 år ved dato for omsorgsovertakelse"),
    VM_1005("1005", "Adopsjon av ektefellens barn"),
    VM_1006("1006", "Mann adopterer ikke alene"),

    VM_1007("1007", "Søknadsfristvilkåret er ikke oppfylt"),

    VM_1019("1019", "Terminbekreftelse utstedt før 22. svangerskapsuke"),

    VM_1020("1020", "Bruker er registrert som ikke medlem"),
    VM_1021("1021", "Bruker er ikke registrert i TPS som bosatt i Norge"),
    VM_1022("1022", "1022"),
    VM_1023("1023", "Bruker ikke er registrert som norsk eller nordisk statsborger i TPS OG bruker ikke er registrert som borger av EU/EØS OG det ikke er avklart at bruker har lovlig opphold i Norge"),
    VM_1024("1024", "Bruker ikke er registrert som norsk eller nordisk statsborger i TPS OG bruker er registrert som borger av EU/EØS OG det ikke er avklart at bruker har oppholdsrett"),
    VM_1025("1025", "Bruker er registrert i TPS som bosatt i Norge OG bruker avklart som ikke bosatt. (Denne kan ha 4 utfall)"),

    VM_1026("1026", "Fødselsdato ikke oppgitt eller registrert"),
    VM_1027("1027", "ingen barn dokumentert på far/medmor"),
    VM_1028("1028", "mor fyller ikke vilkåret for sykdom"),
    VM_1029("1029", "bruker er ikke registrert som far/medmor til barnet"),
    VM_1099("1099", "Ingen beregningsregler tilgjengelig i løsningen (periode utenfor periode på fagsak)"),

    VM_1035("1035", "Ikke tilstrekkelig opptjening"),

    VM_1041("1041", "for lavt brutto beregningsgrunnlag"),

    VM_1051("1051", "Stebarnsadopsjon ikke flere dager igjen"),

    VM_1067("1067", "Ikke dokumentert sykdom, skade eller lyte."),
    VM_1068("1068", "Ikke mottatt dokumentasjon fra rett organ."),
    VM_1069("1069", "Ikke behov for kontinuerlig pleie."),
    VM_1071("1071", "Ikke dokumentert omsorgen for."),

    VM_5007("5007", "søknadsfristvilkåret"),

    VM_7001("7001", "Søker ikke oppfylt opplysningsplikten jf folketrygdloven §§ 21-7 og 21-3"),
    VM_7002("7002", "Start ny vilkårsvurdering"),
    VM_7003("7003", "Søker er medmor (foreldre2) og har søkt på vegne av seg selv"),
    VM_7004("7004", "Søker er far og har søkt på vegne av seg selv"),
    VM_7006("7006", "Venter på opptjeningsopplysninger"),

    VM_8000("8000", "søkt frilans uten frilansinntekt"),
    VM_8001("8001", "avkortet grunnet annen inntekt"),
    VM_8002("8002", "ingen stønadsdager i søknadsperioden"),


    UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VilkårUtfallMerknad> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VILKAR_UTFALL_MERKNAD";

    @JsonIgnore
    private String navn;

    private String kode;

    private VilkårUtfallMerknad(String kode) {
        this.kode = kode;
    }

    private VilkårUtfallMerknad(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static VilkårUtfallMerknad fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(VilkårUtfallMerknad.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårUtfallMerknad: for input " + node);
        }
        return ad;
    }

    public static Map<String, VilkårUtfallMerknad> kodeMap() {
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
