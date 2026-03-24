package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.BarnetilleggSatsTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.GrunnbeløpfaktorTidslinje;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.SatsOgGrunnbeløpfaktor;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestRepositories;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenario;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningInput;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

public class BrevScenarioerUtils {

    public static final String DEFAULT_NAVN = "Aktiv Testesen";
    public static final String DEFAULT_SAKSBEHANDLER_NAVN = "Sara Saksbehandler";
    public static final String SAKSBEHANDLER_2_NAVN = "Siggurd Saksbehandler";
    public static final String DEFAULT_BESLUTTER_NAVN = "Birger Beslutter";

    public static String SAKSBEHANDLER1_IDENT = "SAKSB1";
    public static String SAKSBEHANDLER2_IDENT = "SAKSB2";
    public static String BESLUTTER_IDENT = "BESLUTTER";

    public static final Map<String, String> identNavnMap = Map.of(
        SAKSBEHANDLER1_IDENT, DEFAULT_SAKSBEHANDLER_NAVN,
        SAKSBEHANDLER2_IDENT, SAKSBEHANDLER_2_NAVN,
        BESLUTTER_IDENT, DEFAULT_BESLUTTER_NAVN
    );

    static PersonInformasjon lagBarn(LocalDate barnFødselsdato) {
        return PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).medPersonas().barn(AktørId.dummy(), barnFødselsdato).build();
    }

    static PersonInformasjon lagBarnMedDødsdato(LocalDate barnFødselsdato, LocalDate barnDødsdato) {
        return PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).medPersonas().barn(AktørId.dummy(), barnFødselsdato).dødsdato(barnDødsdato).build();
    }

    public static AktivitetspengerSatsGrunnlag lavSatsGrunnlag(LocalDate fom) {
        return lavSatsGrunnlagMedBarn(fom, 0);
    }

    public static AktivitetspengerSatsGrunnlag lavSatsGrunnlagMedBarn(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.LAV);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        BigDecimal g = hentGrunnbeløpFor(fom);
        return AktivitetspengerSatsGrunnlag.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsBarnetillegg(antallBarn, barneTillegg))
            .build();
    }

    public static AktivitetspengerSatsGrunnlag høySatsGrunnlag(LocalDate fom) {
        return høySatsGrunnlagMedBarn(fom, 0);
    }

    public static AktivitetspengerSatsGrunnlag høySatsGrunnlagMedBarn(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.HØY);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        BigDecimal g = hentGrunnbeløpFor(fom);
        return AktivitetspengerSatsGrunnlag.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsBarnetillegg(antallBarn, barneTillegg))
            .build();
    }

    public static Beregningsgrunnlag lagBeregningsgrunnlag(LocalDate skjæringstidspunkt, BigDecimal beregnetPrÅr) {
        Year sisteLignedeÅr = Year.from(skjæringstidspunkt).minusYears(1);
        BigDecimal beregnetRedusertPrÅr = beregnetPrÅr.multiply(BigDecimal.valueOf(0.66));
        var beregningInput = new BeregningInput(
            new Beløp(beregnetPrÅr),
            new Beløp(beregnetPrÅr),
            new Beløp(beregnetPrÅr),
            skjæringstidspunkt,
            sisteLignedeÅr
        );
        return new Beregningsgrunnlag(beregningInput, beregnetPrÅr, beregnetPrÅr, beregnetPrÅr, beregnetRedusertPrÅr, "regelSporing");
    }

    public static Beregningsgrunnlag lagBeregningsgrunnlagMedLavInntekt(LocalDate skjæringstidspunkt) {
        return lagBeregningsgrunnlag(skjæringstidspunkt, BigDecimal.valueOf(50000));
    }

    public static Beregningsgrunnlag lagBeregningsgrunnlagMedHøyInntekt(LocalDate skjæringstidspunkt) {
        return lagBeregningsgrunnlag(skjæringstidspunkt, BigDecimal.valueOf(600000));
    }

    public static AktivitetspengerSatser lagAktivitetspengerSatser(AktivitetspengerSatsGrunnlag satsGrunnlag, Beregningsgrunnlag beregningsgrunnlag) {
        return new AktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);
    }

    public static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder(LocalDateInterval periode, AktivitetspengerSatser satser) {
        var beregnetSats = satser.hentBeregnetSats();
        BigDecimal dagsats = beregnetSats.dagsats().add(BigDecimal.valueOf(beregnetSats.dagsatsBarnetillegg()));
        var verdi = new TilkjentYtelseVerdi(dagsats, BigDecimal.ZERO, dagsats, dagsats, BigDecimal.valueOf(100), dagsats);
        return new LocalDateTimeline<>(periode, verdi);
    }

    private static BigDecimal hentGrunnbeløpFor(LocalDate fom) {
        return GrunnbeløpTidslinje.hentTidslinje().getSegment(new LocalDateInterval(fom, fom)).getValue().verdi();
    }

    private static int beregnDagsatsBarnetillegg(int antallBarn, BigDecimal barneTillegg) {
        return barneTillegg.multiply(BigDecimal.valueOf(antallBarn)).intValue();
    }

    private static SatsOgGrunnbeløpfaktor hentSatstypeOgGrunnbeløp(Sats sats) {
        return GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), sats)
        ))).stream().findFirst().orElseThrow().getValue();
    }

    public static Behandling lagInnvilgetBehandling(AktivitetspengerTestScenario testscenario, AktivitetspengerTestRepositories repositories) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medAktivitetspengerTestGrunnlag(testscenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        return behandling;
    }

    public static Behandling lagAvsluttetBehandlingMedAP(AktivitetspengerTestScenario testscenario, AktivitetspengerTestRepositories repositories, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var behandling = lagInnvilgetBehandling(testscenario, repositories);
        BehandlingRepository behandlingRepository = repositories.repositoryProvider().getBehandlingRepository();

        leggTilAksjonspunkt(aksjonspunktDefinisjon, behandling);
        behandling.avsluttBehandling();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private static Aksjonspunkt leggTilAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon, Behandling behandling) {
        AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(aksjonspunktDefinisjon);
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "utført");
        return aksjonspunkt;
    }
}
