package no.nav.k9.kodeverk.behandling.aksjonspunkt;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_ANNET;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_ANNET_IKKE_SAKSBEHANDLINGSTID;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_ARBEIDSGIVER;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_SAKSBEHANDLER;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_SØKER;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.Ventekategori.AVVENTER_TEKNISK_FEIL;

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

    UDEFINERT("-", "Ikke definert", false, null),

    ANKE_OVERSENDT_TIL_TRYGDERETTEN("ANKE_OVERSENDT_TIL_TRYGDERETTEN", "Venter på at saken blir behandlet hos Trygderetten", false, AVVENTER_ANNET_IKKE_SAKSBEHANDLINGSTID),
    ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER("ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER", "Tilbakemelding fra søker", true, AVVENTER_SØKER),
    AVV_DOK("AVV_DOK", "Annen dokumentasjon", true, AVVENTER_SØKER),
    AVV_IM_MOT_AAREG("AVV_IM_MOT_AAREG", "Venter på inntektsmelding fra arbeidsgiver som stemmer med Aareg", false, AVVENTER_ARBEIDSGIVER),
    AVV_IM_MOT_SØKNAD_AT("AVV_IM_MOT_SØKNAD_AT", "Venter på inntektsmelding fra arbeidsgiver etter mottatt søknad som arbeidstaker", false, AVVENTER_ARBEIDSGIVER),
    AVV_SØKNADSPERIODER("AVV_SØKNADSPERIODER", "Kan ikke behandle videre før det mottas søknadsperioder fra søknad eller refusjonskrav fra inntektsmelding", false, AVVENTER_SØKER),
    AVV_FODSEL("AVV_FODSEL", "Avventer fødsel", false, AVVENTER_SØKER),
    AVV_RESPONS_REVURDERING("AVV_RESPONS_REVURDERING", "Tilbakemelding på varsel om revurdering", true, AVVENTER_SØKER),
    FOR_TIDLIG_SOKNAD("FOR_TIDLIG_SOKNAD", "Venter pga for tidlig søknad", false, AVVENTER_ANNET_IKKE_SAKSBEHANDLINGSTID),
    GRADERING_FLERE_ARBEIDSFORHOLD("GRADERING_FLERE_ARBEIDSFORHOLD", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette.", false, AVVENTER_TEKNISK_FEIL),
    REFUSJON_3_MÅNEDER("REFUSJON_3_MÅNEDER", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette.", false, AVVENTER_TEKNISK_FEIL),
    SCANN("SCANN", "Venter på scanning", false, AVVENTER_ANNET),
    UTV_FRIST("UTV_FRIST", "Utvidet frist", false, AVVENTER_ANNET),
    VENT_FEIL_ENDRINGSSØKNAD("VENT_FEIL_ENDRINGSSØKNAD", "Behandlingen er satt på vent på grunn av potensielt feil i endringssøknad", false, AVVENTER_TEKNISK_FEIL),
    VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG("VENT_GRADERING_UTEN_BEREGNINGSGRUNNLAG", "Mangel i løsning for oppgitt gradering der utbetaling ikke finnes", false, AVVENTER_TEKNISK_FEIL),
    VENT_INFOTRYGD("VENT_INFOTRYGD", "Venter på en ytelse i Infotrygd", false, AVVENTER_ANNET),
    VENT_INNTEKT_RAPPORTERINGSFRIST("VENT_INNTEKT_RAPPORTERINGSFRIST", "Inntekt rapporteringsfrist", false, AVVENTER_ARBEIDSGIVER),
    VENT_MILITÆR_BG_UNDER_3G("VENT_MILITÆR_OG_BG_UNDER_3G", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette.", false, AVVENTER_TEKNISK_FEIL),
    VENT_OPDT_INNTEKTSMELDING("VENT_OPDT_INNTEKTSMELDING", "Venter på oppdatert inntektsmelding", false, AVVENTER_ARBEIDSGIVER),
    VENT_OPPTJENING_OPPLYSNINGER("VENT_OPPTJENING_OPPLYSNINGER", "Venter på opptjeningsopplysninger", false, AVVENTER_ARBEIDSGIVER), //TODO?
    VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID("VENT_PÅ_NY_INNTEKTSMELDING_MED_GYLDIG_ARB_ID", "Venter på ny inntektsmelding med arbeidsforholdId som stemmer med Aareg", false, AVVENTER_ARBEIDSGIVER),
    VENT_REGISTERINNHENTING("VENT_REGISTERINNHENTING", "Venter på registerinformasjon", false, AVVENTER_ANNET),
    VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT("VENT_PÅ_SISTE_AAP_MELDEKORT", "Venter på siste meldekort for AAP eller dagpenger før første uttaksdag.", false, AVVENTER_SØKER),
    VENT_SØKNAD_SENDT_INFORMASJONSBREV("VENT_SØKNAD_SENDT_INFORMASJONSBREV", "Sendt informasjonsbrev venter søknad.", false, AVVENTER_SØKER),
    VENT_TIDLIGERE_BEHANDLING("VENT_TIDLIGERE_BEHANDLING", "Venter på iverksettelse av en tidligere behandling i denne saken", false, AVVENTER_ANNET),
    VENT_ÅPEN_BEHANDLING("VENT_ÅPEN_BEHANDLING", "Søker eller den andre forelderen har en åpen behandling", false, AVVENTER_ANNET),
    VENT_MANGL_FUNKSJ_SAKSBEHANDLER("VENT_MANGL_FUNKSJ_SAKSBEHANDLER", "Manglende funksjonalitet i løsningen", true, AVVENTER_TEKNISK_FEIL),
    VENTER_SVAR_PORTEN("VENTER_SVAR_PORTEN", "Sak meldt i Porten, venter på svar", true, AVVENTER_TEKNISK_FEIL),
    VENTER_SVAR_TEAMS("VENTER_SVAR_TEAMS", "Sak meldt i Teams, venter på svar", true, AVVENTER_TEKNISK_FEIL),
    ANDRE_INNTEKTSOPPLYSNINGER("ANDRE_INNTEKTSOPPLYSNINGER", "Andre inntektsopplysninger (ikke inntektsmelding)", true, AVVENTER_SØKER), //TODO?
    INNTEKTSMELDING("INNTEKTSMELDING", "Inntektsmelding", true, AVVENTER_ARBEIDSGIVER),
    LEGEERKLÆRING("LEGEERKLÆRING", "Legeerklæring fra riktig lege", true, AVVENTER_SØKER),
    MEDISINSKE_OPPLYSNINGER("MEDISINSKE_OPPLYSNINGER", "Medisinske opplysninger", true, AVVENTER_SØKER),
    ANNET("ANNET", "Annet", true, AVVENTER_ANNET),  //TODO?

    VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER("VENTER_ETTERLYS_IM", "Venter på inntektsmeldinger etter etterlysning", false, AVVENTER_ARBEIDSGIVER),
    VENTER_PÅ_ETTERLYST_INNTEKTSMELDINGER_MED_VARSEL("VENTER_ETTERLYS_IM_VARSEL", "Venter på inntektsmeldinger etter etterlysning med varsel om mulig avslag", false, AVVENTER_ARBEIDSGIVER),

    OPPD_ÅPEN_BEH("OPPD_ÅPEN_BEH", "Venter på oppdatering av åpen behandling", false, AVVENTER_ANNET), //TODO?
    VENT_DEKGRAD_REGEL("VENT_DEKGRAD_REGEL", "Venter på 80% dekningsgrad-regel", false, AVVENTER_TEKNISK_FEIL),
    VENT_ØKONOMI("VENT_ØKONOMI", "Venter på økonomiløsningen", false, AVVENTER_ANNET),
    VENT_TILBAKEKREVING("VENT_TILBAKEKREVING", "Venter på tilbakekrevingsbehandling", true, AVVENTER_ANNET),
    VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER("VENTELØNN_ELLER_MILITÆR_MED_FLERE_AKTIVITETER", "Mangel i løsning for oppgitt ventelønn og/eller militær i kombinasjon med andre statuser", false, AVVENTER_TEKNISK_FEIL),
    VENT_BEREGNING_TILBAKE_I_TID("VENT_BEREGNING_TILBAKE_I_TID", "Endring i tilkjent ytelse bakover i tid. Dette håndteres ikke i løsningen enda.", false, AVVENTER_TEKNISK_FEIL),
    BRUKER_70ÅR_VED_REFUSJON("BRUKER_70ÅR_VED_REFUSJON", "Mangel i løsning for brukere som er 70 år eller eldre", false, AVVENTER_TEKNISK_FEIL),
    VENT_LOVENDRING_8_41("VENT_LOVENDRING_8_41", "Venter på vedtak om lovendring vedrørende beregning av næring i kombinasjon med arbeid eller frilans", false, AVVENTER_TEKNISK_FEIL),

    INGEN_PERIODE_UTEN_YTELSE("INGEN_PERIODE_UTEN_YTELSE", "Mangel i løsning for brukere som har 36 måneder med ytelse før stp.", false, AVVENTER_TEKNISK_FEIL),
    PERIODE_MED_AVSLAG("PERIODE_MED_AVSLAG", "Bruker med avslag for mai-søknad.", false, AVVENTER_TEKNISK_FEIL),
    MANGLENDE_FUNKSJONALITET("MANGLENDE_FUNKSJONALITET", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    KORTVARIG_ARBEID("KORTVARIG_ARBEID", "Søker har kortvarig arbeid siste 6 måneder før skjæringstidspunktet.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_ATFL_SAMME_ORG("FRISINN_ATFL_SAMME_ORG", "Søker er arbeidstaker og frilanser i samme organisasjon og kan ikke behandles. Satt på vent.", false, AVVENTER_TEKNISK_FEIL),

    /**
     * FRISINN VARIANT FILTER - MIDLERTIDIG RUSK TIL VI HAR LANSERT ALT.
     */
    FRISINN_VARIANT_SN_MED_FL_INNTEKT("FRISINN_VARIANT_SN_MED_FL_INNTEKT", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT("FRISINN_VARIANT_FL_MED_SN_INNTEKT", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_NY_FL("FRISINN_VARIANT_NY_FL", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_NY_SN_2019("FRISINN_VARIANT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_NY_SN_2020("FRISINN_VARIANT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_KOMBINERT("FRISINN_VARIANT_KOMBINERT", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_KOMBINERT_NY_FL("FRISINN_VARIANT_KOMBINERT_NY_FL", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2019("FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2019", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2020("FRISINN_VARIANT_KOMBINERT_NY_FL_NY_SN_2020", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_KOMBINERT_NY_SN_2019("FRISINN_VARIANT_KOMBINERT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_KOMBINERT_NY_SN_2020("FRISINN_VARIANT_KOMBINERT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2019("FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2020("FRISINN_VARIANT_SN_MED_FL_INNTEKT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2019("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2019", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2020("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_SN_2020", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2019("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2019", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2020("FRISINN_VARIANT_FL_MED_SN_INNTEKT_NY_FL_NY_SN_2020", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),
    FRISINN_VARIANT_ENDRET_INNTEKTSTYPE("FRISINN_VARIANT_ENDRET_INNTEKTSTYPE", "Mangler funksjonalitet for å behandle saken.", false, AVVENTER_TEKNISK_FEIL),

    // PSB INFOTRYGD MIGRERING
    MANGLER_SØKNAD_FOR_PERIODER_I_INFOTRYGD("AVV_SOKN_IT_PERIODER", "Avventer søknad for alle perioder i infotrygd i inneværende år. Må spesialhåndteres.", false, AVVENTER_SAKSBEHANDLER),
    MANGLER_SØKNADOPPLYSNING_NÆRING("AVV_SOKN_NAERING", "Avventer søknad for næring ved direkte overgang fra infotrygd. Må spesialhåndteres.", false, AVVENTER_SAKSBEHANDLER),
    MANGLER_SØKNADOPPLYSNING_FRILANS("AVV_SOKN_FRILANS", "Avventer søknad for frilans ved direkte overgang fra infotrygd. Må spesialhåndteres.", false, AVVENTER_SAKSBEHANDLER),

    /*
     * Disse kodene kan ikke fjernes før vi eventuelt har ryddet vekk bruk av
     * kodene på eksisterende behandlinger i k9-sak.
     */
    DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP("DELVIS_TILRETTELEGGING_OG_REFUSJON_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette.", false, AVVENTER_TEKNISK_FEIL),
    AAP_DP_SISTE_10_MND_SVP("AAP_DP_SISTE_10_MND_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette.", false, AVVENTER_TEKNISK_FEIL),
    FL_SN_IKKE_STOTTET_FOR_SVP("FL_SN_IKKE_STOTTET_FOR_SVP", "Behandlingen er satt på vent på grunn av mangel i løsningen. Det jobbes med å løse dette.", false, AVVENTER_TEKNISK_FEIL),
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

    private boolean kanVelgesIGui;

    private String kode;

    private Ventekategori ventekategori;

    private Venteårsak(String kode) {
        this.kode = kode;
    }

    private Venteårsak(String kode, String navn, boolean kanVelgesIGui, Ventekategori ventekategori) {
        this.kode = kode;
        this.navn = navn;
        this.kanVelgesIGui = kanVelgesIGui;
        this.ventekategori = ventekategori;
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

    @Override
    public Map<String, String> getEkstraFelter() {
        return Map.of("kanVelges", "" + kanVelgesIGui);
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

    public Ventekategori getVentekategori() {
        return ventekategori;
    }
}
