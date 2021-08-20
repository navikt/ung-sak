package no.nav.k9.kodeverk.behandling.aksjonspunkt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
public enum Venteårsak implements Kodeverdi {

    UDEFINERT("-", "Ikke definert"),

    ANKE_OVERSENDT_TIL_TRYGDERETTEN("ANKE_OVERSENDT_TIL_TRYGDERETTEN", "Venter på at saken blir behandlet hos Trygderetten"),
    ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER("ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER", "Venter på merknader fra bruker"),
    AVV_DOK("AVV_DOK", "Avventer dokumentasjon"),
    AVV_RESPONS_REVURDERING("AVV_RESPONS_REVURDERING", "Avventer respons på varsel om revurdering"),
    FOR_TIDLIG_SOKNAD("FOR_TIDLIG_SOKNAD", "Venter pga for tidlig søknad"),
    GRADERING_FLERE_ARBEIDSFORHOLD("GRADERING_FLERE_ARBEIDSFORHOLD", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    REFUSJON_3_MÅNEDER("REFUSJON_3_MÅNEDER", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    SCANN("SCANN", "Venter på scanning"),
    UTV_FRIST("UTV_FRIST", "Utvidet frist"),
    VENT_FEIL_ENDRINGSSØKNAD("VENT_FEIL_ENDRINGSSØKNAD", "Behandlingen er satt på vent på grunn av potensielt feil i endringssøknad"),
    VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG("VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG", "Mangel i løsning for oppgitt gradering der utbetaling ikke finnes"),
    VENT_INFOTRYGD("VENT_INFOTRYGD", "Venter på en ytelse i Infotrygd"),
    VENT_INNTEKT_RAPPORTERINGSFRIST("VENT_INNTEKT_RAPPORTERINGSFRIST", "Inntekt rapporteringsfrist"),
    VENT_MILITÆR_BG_UNDER_3G("VENT_MILITÆR_OG_BG_UNDER_3G", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette."),
    VENT_OPDT_INNTEKTSMELDING("VENT_OPDT_INNTEKTSMELDING", "Venter på oppdatert inntektsmelding"),
    VENT_OPPTJENING_OPPLYSNINGER("VENT_OPPTJENING_OPPLYSNINGER", "Venter på opptjeningsopplysninger"),
    VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID("VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID", "Venter på ny inntektsmelding med arbeidsforholdId som stemmer med Aareg"),
    VENT_REGISTERINNHENTING("VENT_REGISTERINNHENTING", "Venter på registerinformasjon"),
    VENT_SØKNAD_SENDT_INFORMASJONSBREV("VENT_SØKNAD_SENDT_INFORMASJONSBREV", "Sendt informasjonsbrev venter søknad."),
    VENT_TIDLIGERE_BEHANDLING("VENT_TIDLIGERE_BEHANDLING", "Venter på iverksettelse av en tidligere behandling i denne saken"),
    VENT_ÅPEN_BEHANDLING("VENT_ÅPEN_BEHANDLING", "Søker eller den andre forelderen har en åpen behandling"),
    VENT_MANGL_FUNKSJ_SAKSBEHANDLER("VENT_MANGL_FUNKSJ_SAKSBEHANDLER", "Settes på vent av saksbehandler pga. manglende funksjonalitet i løsningen"),
    VENTER_SVAR_PORTEN("VENTER_SVAR_PORTEN", "Sak meldt i Porten, venter på svar"),
    VENTER_SVAR_TEAMS("VENTER_SVAR_TEAMS", "Sak meldt i Teams, venter på svar"),

    OPPD_ÅPEN_BEH("OPPD_ÅPEN_BEH", "Venter på oppdatering av åpen behandling"),
    VENT_DEKGRAD_REGEL("VENT_DEKGRAD_REGEL", "Venter på 80% dekningsgrad-regel"),
    VENT_ØKONOMI("VENT_ØKONOMI", "Venter på økonomiløsningen"),
    VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER("VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER", "Mangel i løsning for oppgitt ventelønn og/eller militær i kombinasjon med andre statuser"),
    VENT_BEREGNING_TILBAKE_I_TID("VENT_BEREGNING_TILBAKE_I_TID", "Endring i tilkjent ytelse bakover i tid. Dette håndteres ikke i løsningen enda."),
    BRUKER_70ÅR_VED_REFUSJON("BRUKER_70ÅR_VED_REFUSJON", "Mangel i løsning for brukere som er 70 år eller eldre"),

    INGEN_PERIODE_UTEN_YTELSE("INGEN_PERIODE_UTEN_YTELSE", "Mangel i løsning for brukere som har 36 måneder med ytelse før stp."),
    PERIODE_MED_AVSLAG("PERIODE_MED_AVSLAG", "Bruker med avslag for mai-søknad."),
    MANGLENDE_FUNKSJONALITET("MANGLENDE_FUNKSJONALITET", "Mangler funksjonalitet for å behandle saken."),
    KORTVARIG_ARBEID("KORTVARIG_ARBEID", "Søker har kortvarig arbeid siste 6 måneder før skjæringstidspunktet."),
    FRISINN_ATFL_SAMME_ORG("FRISINN_ATFL_SAMME_ORG", "Søker er arbeidstaker og frilanser i samme organisasjon og kan ikke behandles. Satt på vent."),

    /** FRISINN VARIANT FILTER - MIDLERTIDIG RUSK TIL VI HAR LANSERT ALT. */
    FRISINN_VARIANT_SN_MED_FL_INNTEKT("FRISINN_VARIANT_SN_MED_FL_INNTEKT", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT("FRISINN_VARIANT_FL_MED_SN_INNTEKT", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_NY_FL("FRISINN_VARIANT_NY_FL", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_NY_SN_2019("FRISINN_VARIANT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_NY_SN_2020("FRISINN_VARIANT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_KOMBINERT("FRISINN_VARIANT_KOMBINERT", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_KOMBINERT_NY_FL("FRISINN_VARIANT_KOMBINERT_NY_FL", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2019("FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2019", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2020("FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2020", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_KOMBINERT_NY_SN_2019("FRISINN_VARIANT_KOMBINERT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_KOMBINERT_NY_SN_2020("FRISINN_VARIANT_KOMBINERT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2019("FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2020("FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2019("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2020("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2019("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2019", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2020("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2020", "Mangler funksjonalitet for å behandle saken."),
    FRISINN_VARIANT_ENDRET_INNTEKTSTYPE("FRISINN_VARIANT_ENDRET_INNTEKTSTYPE", "Mangler funksjonalitet for å behandle saken."),
    ;
    public static final String KODEVERK = "VENT_AARSAK";
    private static final Map<String, Venteårsak> KODER = new LinkedHashMap<>();

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

    private Venteårsak(String kode) {
        this.kode = kode;
    }

    private Venteårsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static Venteårsak fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Venteårsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Venteårsak: " + kode);
        }
        return ad;
    }

    public static Map<String, Venteårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(a -> "\"" + a + "\"").collect(Collectors.toList()));
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

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }
}
