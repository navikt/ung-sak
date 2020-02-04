package no.nav.k9.kodeverk.uttak;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.k9.kodeverk.uttak.IkkeOppfyltÅrsak.MyIkkeOppfyltPeriodeResultatÅrsakSerializer;

@JsonDeserialize(using=PeriodeResultatÅrsakDeserializer.class)
@JsonSerialize(using=MyIkkeOppfyltPeriodeResultatÅrsakSerializer.class)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum IkkeOppfyltÅrsak implements PeriodeResultatÅrsak {

    UKJENT("-", "Ikke definert", null),
    HULL_MELLOM_FORELDRENES_PERIODER("4005", "§14-10 sjuende ledd: Ikke sammenhengende perioder",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    IKKE_STØNADSDAGER_IGJEN("4002", "§14-9: Ikke stønadsdager igjen på stønadskonto", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}"),
    SØKNADSFRIST("4020", "§22-13 tredje ledd: Brudd på søknadsfrist", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"22-13\"}}}"),
    BARN_OVER_3_ÅR("4022", "§14-10 tredje ledd: Barnet er over 3 år", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    ARBEIDER_I_UTTAKSPERIODEN_MER_ENN_0_PROSENT("4023", "§14-10 femte ledd: Arbeider i uttaksperioden mer enn 0%",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    AVSLAG_GRADERING_ARBEIDER_100_PROSENT_ELLER_MER("4025", "§14-16 første ledd: Avslag gradering - arbeid 100% eller mer",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}"),
    AVSLAG_GRADERING_SØKER_ER_IKKE_I_ARBEID("4093", "§14-16: Avslag gradering - søker er ikke i arbeid",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}"),
    FAR_HAR_IKKE_OMSORG("4012", "§14-10 fjerde ledd: Far/medmor har ikke omsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    MOR_HAR_IKKE_OMSORG("4003", "§14-10 fjerde ledd: Mor har ikke omsorg", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    MOR_SØKER_FELLESPERIODE_FØR_12_UKER_FØR_TERMIN_FØDSEL("4013", "§14-10 første ledd: Mor søker uttak før 12 uker før termin/fødsel",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    SAMTIDIG_UTTAK_IKKE_GYLDIG_KOMBINASJON("4060", "§14-10 sjette ledd: Samtidig uttak - ikke gyldig kombinasjon",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    MOR_IKKE_RETT_TIL_FORELDREPENGER("4073", "§14-12 første ledd: Ikke rett til kvote fordi mor ikke har rett til foreldrepenger",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}"),
    BARNET_ER_DØD("4072", "§14-9 sjuende ledd: Barnet er dødt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}"),
    SØKER_ER_DØD("4071", "§14-10: Bruker er død", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIT_UTTAK("4084",
            "§14-10 sjette ledd: Annen part har overlappende uttak, det er ikke søkt/innvilget samtidig uttak",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    IKKE_SAMTYKKE_MELLOM_PARTENE("4085", "§14-10 sjette ledd: Det er ikke samtykke mellom partene",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE("4086",
            "§14-10 sjette ledd og §14-11: Annen part har overlappende uttaksperioder som er innvilget utsettelse",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10,14-11\"}}}"),
    MOR_TAR_IKKE_ALLE_UKENE("4095", "§14-10 første ledd: Mor tar ikke alle 3 ukene før termin",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}"),
    OPPHØR_MEDLEMSKAP("4087", "§14-2: Opphør medlemskap", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-2\"}}}"),
    FØDSELSVILKÅRET_IKKE_OPPFYLT("4096", "§14-5: Fødselsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}"),
    ADOPSJONSVILKÅRET_IKKE_OPPFYLT("4097", "§14-5: Adopsjonsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}"),
    FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT("4098", "§14-5: Foreldreansvarsvilkåret er ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}"),
    OPPTJENINGSVILKÅRET_IKKE_OPPFYLT("4099", "§14-6: Opptjeningsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-6\"}}}"),
    AKTIVITETSKRAVET_ARBEID_IKKE_OPPFYLT("4050", "§14-13 første ledd bokstav a: Aktivitetskravet arbeid ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}"),
    AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_IKKE_OPPFYLT("4051",
            "§14-13 første ledd bokstav b: Aktivitetskravet offentlig godkjent utdanning ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}"),
    AKTIVITETSKRAVET_OFFENTLIG_GODKJENT_UTDANNING_I_KOMBINASJON_MED_ARBEID_IKKE_OPPFYLT("4052",
            "§14-13 første ledd bokstav c: Aktivitetskravet offentlig godkjent utdanning i kombinasjon med arbeid ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}"),
    AKTIVITETSKRAVET_MORS_SYKDOM_IKKE_OPPFYLT("4053", "§14-13 første ledd bokstav d: Aktivitetskravet mors sykdom/skade ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}"),
    AKTIVITETSKRAVET_MORS_INNLEGGELSE_IKKE_OPPFYLT("4054", "§14-13 første ledd bokstav e: Aktivitetskravet mors innleggelse ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}"),
    AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_INTRODUKSJONSPROGRAM_IKKE_OPPFYLT("4055",
            "§14-13 første ledd bokstav f: Aktivitetskravet mors deltakelse på introduksjonsprogram ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}"),
    AKTIVITETSKRAVET_MORS_DELTAKELSE_PÅ_KVALIFISERINGSPROGRAM_IKKE_OPPFYLT("4056",
            "§14-13 første ledd bokstav g: Aktivitetskravet mors deltakelse på kvalifiseringsprogram ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13\"}}}"),
    MORS_MOTTAK_AV_UFØRETRYGD_IKKE_OPPFYLT("4057", "§14-14 tredje ledd: Unntak for aktivitetskravet, mors mottak av uføretrygd ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-14\"}}}"),
    STEBARNSADOPSJON_IKKE_NOK_DAGER("4058", "§14-5 tredje ledd: Unntak for Aktivitetskravet, stebarnsadopsjon - ikke nok dager",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-5\"}}}"),
    FLERBARNSFØDSEL_IKKE_NOK_DAGER("4059", "§14-13 sjette ledd, jf. §14-9 fjerde ledd: Unntak for Aktivitetskravet, flerbarnsfødsel - ikke nok dager",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13, 14-9\"}}}"),
    AKTIVITETSKRAVET_ARBEID_IKKE_DOKUMENTERT("4066", "§14-13 første ledd bokstav a, jf §21-3: Aktivitetskrav - arbeid ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}"),
    AKTIVITETSKRAVET_UTDANNING_IKKE_DOKUMENTERT("4067", "§14-13 første ledd bokstav b, jf §21-3: Aktivitetskrav – utdanning ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}"),
    AKTIVITETSKRAVET_ARBEID_I_KOMB_UTDANNING_IKKE_DOKUMENTERT("4068",
            "§14-13 første ledd bokstav c, jf §21-3: Aktivitetskrav – arbeid i komb utdanning ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}"),
    AKTIVITETSKRAVET_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT("4069",
            "§14-13 første ledd bokstav d og femte ledd, jf §21-3: Aktivitetskrav – sykdom/skade ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}"),
    AKTIVITETSKRAVET_INNLEGGELSE_IKKE_DOKUMENTERT("4070", "§14-13 første ledd bokstav e og femte ledd, jf §21-3: Aktivitetskrav – innleggelse ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}"),
    AKTIVITETSKRAVET_INTROPROGRAM_IKKE_DOKUMENTERT("4088", "§14-13 første ledd bokstav f, jf §21-3: Aktivitetskrav – introprogram ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}"),
    AKTIVITETSKRAVET_KVP_IKKE_DOKUMENTERT("4089", "§14-13 første ledd bokstav g, jf §21-3: Aktivitetskrav – KVP ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-13,21-3\"}}}"),
    DEN_ANDRE_PART_SYK_SKADET_IKKE_OPPFYLT("4007", "§14-12 tredje ledd: Den andre part syk/skadet ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}"),
    DEN_ANDRE_PART_INNLEGGELSE_IKKE_OPPFYLT("4008", "§14-12 tredje ledd: Den andre part innleggelse ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}"),
    SYKDOM_SKADE_INNLEGGELSE_IKKE_DOKUMENTERT("4074", "§14-12 tredje ledd, jf §21-3: Avslag overføring kvote pga. sykdom/skade/innleggelse ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12,21-3\"}}}"),
    HAR_IKKE_ALENEOMSORG_FOR_BARNET("4092", "§14-12: Avslag overføring - har ikke aleneomsorg for barnet",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-12\"}}}"),
    IKKE_LOVBESTEMT_FERIE("4033", "§14-11 første ledd bokstav a: Ikke lovbestemt ferie", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    FERIE_SELVSTENDIG_NÆRINGSDRIVENDSE_FRILANSER("4032", "§14-11 første ledd bokstav a: Ferie - selvstendig næringsdrivende/frilanser",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    IKKE_HELTIDSARBEID("4037", "§14-11 første ledd bokstav b: Ikke heltidsarbeid", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    SØKERS_SYKDOM_SKADE_IKKE_OPPFYLT("4038", "§14-11 første ledd bokstav c: Søkers sykdom/skade ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    SØKERS_INNLEGGELSE_IKKE_OPPFYLT("4039", "§14-11 første ledd bokstav c: Søkers innleggelse ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    BARNETS_INNLEGGELSE_IKKE_OPPFYLT("4040", "§14-11 første ledd bokstav d: Barnets innleggelse ikke oppfylt",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    FERIE_INNENFOR_DE_FØRSTE_6_UKENE("4031", "§14-9: Ferie/arbeid innenfor de første 6 ukene",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}"),
    INGEN_STØNADSDAGER_IGJEN("4034", "§14-11, jf §14-9: Avslag utsettelse - ingen stønadsdager igjen",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-9\"}}}"),
    BARE_FAR_RETT_MOR_FYLLES_IKKE_AKTIVITETSKRAVET("4035", "§14-11 første ledd bokstav b, jf. §14-14: Bare far har rett, mor fyller ikke aktivitetskravet",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,14-14,14-13\"}}}"),
    UTSETTELSE_FØR_TERMIN_FØDSEL("4030", "§14-9: Avslag utsettelse før termin/fødsel", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-9\"}}}"),
    UTSETTELSE_FERIE_PÅ_BEVEGELIG_HELLIGDAG("4041", "§14-11 første ledd bokstav a: Avslag utsettelse ferie på bevegelig helligdag",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    UTSETTELSE_FERIE_IKKE_DOKUMENTERT("4061", "§14-11 første ledd bokstav a, jf §21-3: Utsettelse ferie ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}"),
    UTSETTELSE_ARBEID_IKKE_DOKUMENTERT("4062", "§14-11 første ledd bokstav b, jf §21-3: Utsettelse arbeid ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}"),
    UTSETTELSE_SØKERS_SYKDOM_ELLER_SKADE_IKKE_DOKUMENTERT("4063",
            "§14-11 første ledd bokstav c og tredje ledd, jf §21-3: Utsettelse søkers sykdom/skade ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}"),
    UTSETTELSE_SØKERS_INNLEGGELSE_IKKE_DOKUMENTERT("4064",
            "§14-11 første ledd bokstav c og tredje ledd, jf §21-3: Utsettelse søkers innleggelse ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}"),
    UTSETTELSE_BARNETS_INNLEGGELSE_IKKE_DOKUMENTERT("4065", "§14-11 første ledd bokstav d, jf §21-3: Utsettelse barnets innleggelse - ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11,21-3\"}}}"),
    AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID("4081", "§14-11 første ledd bokstav a: Avslag utsettelse pga ferie tilbake i tid",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID("4082", "§14-11 første ledd bokstav b: Avslag utsettelse pga arbeid tilbake i tid",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-11\"}}}"),
    HULL_MELLOM_SØKNADSPERIOD_ETTER_SISTE_UTSETTELSE("4091", "§14-10 sjuende ledd: Ikke sammenhengende perioder",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", LocalDate.of(2001,01,01)),
    HULL_MELLOM_SØKNADSPERIODE_ETTER_SISTE_UTTAK("4090", "§14-10 sjuende ledd: Ikke sammenhengende perioder",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10\"}}}", LocalDate.of(2001,01,01)),
    FRATREKK_PLEIEPENGER("4077", "§14-10 a: Innvilget prematuruker, med fratrekk pleiepenger",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-10 a\"}}}"),
    AVSLAG_GRADERING_PÅ_GRUNN_AV_FOR_SEN_SØKNAD("4080", "§14-16: Ikke gradering pga. for sen søknad",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}", LocalDate.of(2001,01,01)),
    AVSLAG_GRADERINGSAVTALE_MANGLER_IKKE_DOKUMENTERT("4094", "§14-16 femte ledd, jf §21-3: Avslag graderingsavtale mangler - ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16,21-3\"}}}", LocalDate.of(2001,01,01)),
    _4018("4018", "§14-10 andre ledd: Søkt uttak/utsettelse før omsorgsovertakelse", "", LocalDate.of(2001,01,01)),
    _4006("4006", "§14-10 sjuende ledd: Ikke sammenhengende perioder", "", LocalDate.of(2001,01,01)),
    _4100("4100", "§14-10 andre ledd: Uttak før omsorgsovertakelse", ""),
    _4075("4075", "§14-9 første ledd: Ikke rett til fellesperiode fordi mor ikke har rett til foreldrepenger", ""),
    _4076("4076", "§14-9 femte ledd: Avslag overføring - annen forelder har rett til foreldrepenger", ""),
    ;

    private static final Map<String, IkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "IKKE_OPPFYLT_AARSAK";

    @Deprecated
    public static final String DISCRIMINATOR = "IKKE_OPPFYLT_AARSAK";

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
    private String lovHjemmel;
    
    @JsonIgnore
    private LocalDate gyldigFom;
    @JsonIgnore
    private LocalDate gyldigTom;
    
    private IkkeOppfyltÅrsak(String kode) {
        this.kode = kode;
    }

    private IkkeOppfyltÅrsak(String kode, String navn, String lovHjemmel) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
        this.gyldigFom = LocalDate.of(2000, 01, 01);
        this.gyldigTom = Tid.TIDENES_ENDE;
    }

    private IkkeOppfyltÅrsak(String kode, String navn, String lovHjemmel, LocalDate gyldigTom) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
        this.gyldigFom = LocalDate.of(2000, 01, 01);
        this.gyldigTom = gyldigTom;
    }
    
    @JsonProperty("gyldigFom")
    @Override
    public LocalDate getGyldigFraOgMed() {
        return gyldigFom;
    }
    
    @JsonProperty("gyldigTom")
    @Override
    public LocalDate getGyldigTilOgMed() {
        return gyldigTom;
    }

    @JsonCreator
    public static IkkeOppfyltÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent IkkeOppfyltÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, IkkeOppfyltÅrsak> kodeMap() {
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

    /** Returnerer p.t. Raw json. */
    @Override
    public String getLovHjemmelData() {
        return lovHjemmel;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(k -> "'" + k + "'").collect(Collectors.toList()));
    }

    public static Set<PeriodeResultatÅrsak> opphørsAvslagÅrsaker() {
        return new HashSet<>(Arrays.asList(
            MOR_HAR_IKKE_OMSORG,
            FAR_HAR_IKKE_OMSORG,
            BARNET_ER_DØD,
            SØKER_ER_DØD,
            OPPHØR_MEDLEMSKAP,
            FØDSELSVILKÅRET_IKKE_OPPFYLT,
            ADOPSJONSVILKÅRET_IKKE_OPPFYLT,
            FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT,
            OPPTJENINGSVILKÅRET_IKKE_OPPFYLT));
    }

    public static Set<PeriodeResultatÅrsak> årsakerTilAvslagPgaAnnenpart() {
        return new HashSet<>(Arrays.asList(
            DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIT_UTTAK,
            DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE));
    }
    
    public static class MyIkkeOppfyltPeriodeResultatÅrsakSerializer extends PeriodeResultatÅrsakSerializer<IkkeOppfyltÅrsak> {
        public MyIkkeOppfyltPeriodeResultatÅrsakSerializer() {
            super(IkkeOppfyltÅrsak.class);
        }
    }
}
