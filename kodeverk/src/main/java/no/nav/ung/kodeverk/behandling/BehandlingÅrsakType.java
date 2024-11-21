package no.nav.ung.kodeverk.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
public enum BehandlingÅrsakType implements Kodeverdi {

    RE_MANGLER_FØDSEL("RE-MF", "Manglende informasjon om fødsel i folkeregisteret"),
    RE_MANGLER_FØDSEL_I_PERIODE("RE-MFIP", "Manglende informasjon om fødsel i folkeregisteret mellom uke 26 og 29"),
    RE_AVVIK_ANTALL_BARN("RE-AVAB", "Avvik i antall barn"),
    RE_FEIL_I_LOVANDVENDELSE("RE-LOV", "Feil lovanvendelse"),
    RE_FEIL_REGELVERKSFORSTÅELSE("RE-RGLF", "Feil regelverksforståelse"),
    RE_FEIL_ELLER_ENDRET_FAKTA("RE-FEFAKTA", "Feil eller endret fakta"),
    RE_FEIL_PROSESSUELL("RE-PRSSL", "Prosessuell feil"),
    RE_ENDRING_FRA_BRUKER("RE-END-FRA-BRUKER", "Endring fra bruker"),
    RE_FRAVÆRSKORRIGERING_FRA_SAKSBEHANDLER("RE-FRAVÆRKORR-SAKSB", "Fraværskorrigering fra saksbehandler"),
    BERØRT_BEHANDLING("BERØRT-BEHANDLING", "Endring i den andre forelderens uttak"),
    RE_ANNET("RE-ANNET", "Annet"),
    RE_SATS_REGULERING("RE-SATS-REGULERING", "Regulering av grunnbeløp"),
    RE_ENDRET_FORDELING("RE-ENDRET-FORDELING", "Endring som kun påvirker fordeling, avkorting og reduksjon av ytelse"),
    //For automatiske informasjonsbrev
    INFOBREV_BEHANDLING("INFOBREV_BEHANDLING", "Sende informasjonsbrev"),
    INFOBREV_OPPHOLD("INFOBREV_OPPHOLD", "Sende informasjonsbrev om opphold det ikke er søkt om"),

    // Manuelt opprettet revurdering (obs: årsakene kan også bli satt på en automatisk opprettet revurdering)
    RE_KLAGE_UTEN_END_INNTEKT("RE-KLAG-U-INNTK", "Klage/ankebehandling uten endrede inntektsopplysninger"),
    RE_KLAGE_MED_END_INNTEKT("RE-KLAG-M-INNTK", "Klage/ankebehandling med endrede inntektsopplysninger"),
    RE_OPPLYSNINGER_OM_MEDLEMSKAP("RE-MDL", "Nye opplysninger om medlemskap"),
    RE_OPPLYSNINGER_OM_OPPTJENING("RE-OPTJ", "Nye opplysninger om opptjening"),
    RE_OPPLYSNINGER_OM_FORDELING("RE-FRDLING", "Nye opplysninger om uttak"),
    RE_OPPLYSNINGER_OM_INNTEKT("RE-INNTK", "Nye opplysninger om inntekt"),
    RE_OPPLYSNINGER_OM_DØD("RE-DØD", "Dødsfall"),
    RE_OPPLYSNINGER_OM_SØKERS_REL("RE-SRTB", "Nye opplysninger om søkers relasjon til barnet"),
    RE_OPPLYSNINGER_OM_SØKNAD_FRIST("RE-FRIST", "Nye opplysninger som kan påvirke vurderingen av søknadsfristen"),
    RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG("RE-BER-GRUN", "Nye opplysninger som kan påvirke beregningsgrunnlaget"),
    RE_KLAGE_NY_INNH_LIGNET_INNTEKT("RE-KLAG-INNH-PGI", "Klage/ankebehandling med ny innhenting av lignet inntekt"),
    RE_KLAGE_NATTEVÅKBEREDSKAP("RE-NB", "Nye opplysninger om nattevåk/beredskap etter klage"),
    ETTER_KLAGE("ETTER_KLAGE", "Ny behandling eller revurdering etter klage eller anke"),

    RE_HENDELSE_FØDSEL("RE-HENDELSE-FØDSEL", "Melding om registrert fødsel i folkeregisteret"),
    RE_HENDELSE_DØD_FORELDER("RE-HENDELSE-DØD-F", "Melding om registrert død på bruker i folkeregisteret"),
    RE_HENDELSE_DØD_BARN("RE-HENDELSE-DØD-B", "Melding om registrert død på pleietrengende i folkeregisteret"),
    RE_HENDELSE_DØDFØDSEL("RE-HENDELSE-DØDFØD", "Melding om registrert dødfødsel i folkeregisteret"),
    RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM("RE-HENDELSE-OPPHØR-UNG", "Melding om registrert opphør av ungdomsprogram for bruker"),

    RE_REGISTEROPPLYSNING("RE-REGISTEROPPL", "Nye registeropplysninger"),
    RE_OPPLYSNINGER_OM_YTELSER("RE-YTELSE", "Nye opplysninger om ytelse"),
    RE_TILSTØTENDE_YTELSE_INNVILGET("RE-TILST-YT-INNVIL", "Tilstøtende ytelse innvilget"),
    RE_ENDRING_BEREGNINGSGRUNNLAG("RE-ENDR-BER-GRUN", "Nye opplysninger som kan påvirke beregningsgrunnlaget"),
    RE_TILSTØTENDE_YTELSE_OPPHØRT("RE-TILST-YT-OPPH", "Tilstøtende ytelse opphørt"),
    RE_REBEREGN_FERIEPENGER("RE_REBEREGN_FP", "Reberegning av feriepenger"),

    RE_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK", "Nye opplysninger fra annen omsorgsperson"),
    RE_UTSATT_BEHANDLING("RE_UTSATT_BEHANDLING", "Utsatt behandling av periode på grunn av avhengighet til annen omsorgspersons uttak"),
    RE_GJENOPPTAR_UTSATT_BEHANDLING("RE_GJENOPPTAR_UTSATT_BEHANDLING", "Gjenopptar utsatt behandling av periode fra forrige behandling"),
    RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK_ET", "Nye opplysninger om etablert tilsyn"),
    RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK_NB", "Nye opplysninger om nattevåk/beredskap fra annen omsorgsperson"),
    RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK_S", "Nye opplysninger om sykdom"),
    RE_NATTEVÅKBEREDSKAP_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK_NB_ET", "Nye opplysninger om etablert tilsyn og nattevåk/beredskap"),
    RE_SYKDOM_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK_S_ET", "Nye opplysninger om sykdom og etablert tilsyn"),
    RE_SYKDOM_NATTEVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK_S_NB", "Nye opplysninger om sykdom og nattevåk/beredskap"),
    RE_SYKDOM_ETABLERT_TILSYN_NATTVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON("RE_ANNEN_SAK_S_ET_NB", "Nye opplysninger om sykdom, nattevåk/beredskap og etablert tilsyn "),
    RE_FERIEPENGER_ENDRING_FRA_ANNEN_SAK("RE_ANNEN_SAK_FP", "Reberegner feriepenger pga endring i annen sak"),

    // Unntaksbehandling
    UNNT_GENERELL("UNNT_GENERELL", "Manuell saksbehandling"),
    REVURDERER_BERØRT_PERIODE("REVURDERER_BERØRT_PERIODE", "Revurderer berørt periode"),

    //ungdomsytelsespesifikt
    RE_TRIGGER_BEREGNING_HØY_SATS("RE_TRIGGER_BEREGNING_HØY_SATS", "Beregn høy sats"),

    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "BEHANDLING_AARSAK"; //$NON-NLS-1$

    private static final Map<String, BehandlingÅrsakType> KODER = new LinkedHashMap<>();

    public static final Set<BehandlingÅrsakType> ANNEN_OMSORGSPERSON_TYPER = Set.of(BehandlingÅrsakType.RE_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        RE_NATTEVÅKBEREDSKAP_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        RE_SYKDOM_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        RE_NATTEVÅKBEREDSKAP_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        RE_SYKDOM_ETABLERT_TILSYN_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        RE_SYKDOM_NATTEVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON,
        RE_SYKDOM_ETABLERT_TILSYN_NATTVÅK_ENDRING_FRA_ANNEN_OMSORGSPERSON);

    public static final boolean medførerVilkårsperioder(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsakType != RE_FERIEPENGER_ENDRING_FRA_ANNEN_SAK && behandlingÅrsakType != RE_REBEREGN_FERIEPENGER;
    }

    @JsonIgnore
    private String navn;

    private String kode;

    private BehandlingÅrsakType(String kode) {
        this.kode = kode;
    }

    private BehandlingÅrsakType(String kode, String navn) {
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
    public static BehandlingÅrsakType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BehandlingÅrsakType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingÅrsakType: for input " + node);
        }
        return ad;
    }

    public static Map<String, BehandlingÅrsakType> kodeMap() {
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

    public static Set<BehandlingÅrsakType> årsakerForAutomatiskRevurdering() {
        return Set.of(RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN,
            RE_TILSTØTENDE_YTELSE_INNVILGET, RE_ENDRING_BEREGNINGSGRUNNLAG, RE_TILSTØTENDE_YTELSE_OPPHØRT);
    }

    public static Set<BehandlingÅrsakType> årsakerForEtterkontroll() {
        return Set.of(RE_MANGLER_FØDSEL, RE_MANGLER_FØDSEL_I_PERIODE, RE_AVVIK_ANTALL_BARN);
    }

    public static Set<BehandlingÅrsakType> årsakerEtterKlageBehandling() {
        return Set.of(ETTER_KLAGE, RE_KLAGE_MED_END_INNTEKT, RE_KLAGE_UTEN_END_INNTEKT);
    }
}
