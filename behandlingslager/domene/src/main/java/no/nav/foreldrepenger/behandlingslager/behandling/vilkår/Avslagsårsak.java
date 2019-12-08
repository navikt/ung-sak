package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.ÅrsakskodeMedLovreferanse;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum Avslagsårsak implements Kodeverdi, ÅrsakskodeMedLovreferanse{

    SØKT_FOR_SENT("1007", "Søkt for sent", null),
    MANGLENDE_DOKUMENTASJON("1019", "Manglende dokumentasjon", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_34\", \"lovreferanse\": \"21-3,21-7\"}]}]}"),
    SØKER_ER_IKKE_MEDLEM("1020", "Søker er ikke medlem", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_ER_UTVANDRET("1021", "Søker er utvandret", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_HAR_IKKE_LOVLIG_OPPHOLD("1023", "Søker har ikke lovlig opphold", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_HAR_IKKE_OPPHOLDSRETT("1024", "Søker har ikke oppholdsrett", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    SØKER_ER_IKKE_BOSATT("1025", "Søker er ikke bosatt", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_2\", \"lovreferanse\": \"14-2\"}]}]}"),
    IKKE_TILSTREKKELIG_OPPTJENING("1035", "Ikke tilstrekkelig opptjening", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_23\", \"lovreferanse\": \"14-6\"}]}]}"),
    FOR_LAVT_BEREGNINGSGRUNNLAG("1041", "For lavt brutto beregningsgrunnlag", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_41\", \"lovreferanse\": \"14-7\"}]}]}"),
    STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN("1051", "Stebarnsadopsjon ikke flere dager igjen", "{\"fagsakYtelseType\": [{\"FP\": [{\"kategori\": \"FP_VK_16\", \"lovreferanse\": \"14-5\"}]}]}"),
    SØKER_IKKE_GRAVID_KVINNE("1060", "§14-4 første ledd: Søker er ikke gravid kvinne", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 1. ledd\"}]}]}"),
    SØKER_ER_IKKE_I_ARBEID("1061", "§14-4 tredje ledd: Søker er ikke i arbeid/har ikke tap av pensjonsgivende inntekt", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 3. ledd\"}]}]}"),
    ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER("1063", "§14-4 første ledd: Arbeidstaker har ikke dokumentert risikofaktorer", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 1. ledd\"}]}]}"),
    ARBEIDSTAKER_KAN_OMPLASSERES("1064", "§14-4 første ledd: Arbeidstaker kan omplasseres til annet høvelig arbeid", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 1. ledd\"}]}]}"),
    SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER("1065", "§14-4 andre ledd: Næringsdrivende/frilanser har ikke dokumentert risikofaktorer", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 2. ledd\"}]}]}"),
    SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE("1066", "§14-4 andre ledd: Næringsdrivende/frilanser har mulighet til å tilrettelegge sitt virke", "{\"fagsakYtelseType\": [{\"SVP\": [{\"kategori\": \"SVP_VK_1\", \"lovreferanse\": \"14-4 2. ledd\"}]}]}"),
    INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN("1099", "Ingen beregningsregler tilgjengelig i løsningen", null),
    UDEFINERT("-", "Ikke definert", null),

    ;
    
    private static final Map<String, Avslagsårsak> KODER = new LinkedHashMap<>();
    
    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public static final String KODEVERK = "AVSLAGSARSAK"; //$NON-NLS-1$
    
    // TODO endre fra raw json
    @JsonIgnore
    private String lovReferanse;

    @JsonIgnore
    private String navn;

    private String kode;

    private Avslagsårsak(String kode, String navn, String lovReferanse) {
        this.kode = kode;
        this.navn = navn;
        this.lovReferanse = lovReferanse;
    }

    @JsonCreator
    public static Avslagsårsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Avslagsårsak: " + kode);
        }
        return ad;
    }
    
    public static Map<String, Avslagsårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }
    
    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }
    
    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }
    
    /**
     * Get vilkår dette avslaget kan opptre i.
     */
    public Set<VilkårType> getVilkårTyper(){
        return VilkårType.getVilkårTyper(this);
    }

    @Override
    public String getLovHjemmelData() {
        return lovReferanse;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(a -> "\"" + a + "\"").collect(Collectors.toList()));
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Avslagsårsak, String> {
        @Override
        public String convertToDatabaseColumn(Avslagsårsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Avslagsårsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }


}
