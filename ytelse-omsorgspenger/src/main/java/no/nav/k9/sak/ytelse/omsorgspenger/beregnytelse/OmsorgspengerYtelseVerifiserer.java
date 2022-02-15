package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;

@Dependent
public class OmsorgspengerYtelseVerifiserer {

    private static final Logger logger = LoggerFactory.getLogger(OmsorgspengerYtelseVerifiserer.class);

    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;
    private Boolean skruPåNyVerifisering;

    @Inject
    public OmsorgspengerYtelseVerifiserer(TrekkUtFraværTjeneste trekkUtFraværTjeneste,
                                          @KonfigVerdi(value = "OMP_VERIFISERING_TILKJENT_YTELSE", defaultVerdi = "true") Boolean skruPåNyVerifisering) {
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
        this.skruPåNyVerifisering = skruPåNyVerifisering;
    }

    public void verifiser(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);
        if (skruPåNyVerifisering) {
            verifiserUtbetalingKunHvorKrav(behandling, beregningsresultat);
        }
    }

    private void verifiserUtbetalingKunHvorKrav(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        verifiserRefusjonKunVedRefusjonskrav(behandling, beregningsresultat);
        verifiserUtbetalingSøkerKunVedSøknad(behandling, beregningsresultat);
    }

    private void verifiserRefusjonKunVedRefusjonskrav(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        List<OppgittFraværPeriode> fraværsperioder = trekkUtFraværTjeneste.fraværFraInntektsmeldingerPåFagsak(behandling);
        LocalDateTimeline<Set<Arbeidsgiver>> imKravTidslinje = kravTidslinje(fraværsperioder);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingRefusjonTidslinje = tidslinjeUtbetaling(beregningsresultat, false);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingUtenKrav = finnUtbetalingUtenKrav(imKravTidslinje, utbetalingRefusjonTidslinje);

        if (!utbetalingUtenKrav.isEmpty()) {
            var perioder = utbetalingUtenKrav.stream().map(LocalDateSegment::getLocalDateInterval).toList();
            if (utbetalingUtenKrav.getMinLocalDate().getYear() >= 2022) {
                throw new IllegalArgumentException("Feil i løsningen. Har tilkjent refusjon uten refusjonskrav. Gjelder en eller flere arbeidsgivere for periodene " + perioder);
            } else {
                logger.warn("Har tilkjent refusjon uten refusjonskrav. Gjelder en eller flere arbeidsgivere for periodene {}", perioder);
            }
        }
    }

    private void verifiserUtbetalingSøkerKunVedSøknad(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        List<OppgittFraværPeriode> fraværsperioder = trekkUtFraværTjeneste.fraværsperioderFraSøknaderPåFagsak(behandling);
        LocalDateTimeline<Set<Arbeidsgiver>> søknadTidslinje = kravTidslinje(fraværsperioder);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingTilSøkerTidslinje = tidslinjeUtbetaling(beregningsresultat, true);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingUtenKrav = finnUtbetalingUtenKrav(søknadTidslinje, utbetalingTilSøkerTidslinje);

        if (!utbetalingUtenKrav.isEmpty()) {
            var perioder = utbetalingUtenKrav.stream().map(LocalDateSegment::getLocalDateInterval).toList();
            if (utbetalingUtenKrav.getMinLocalDate().getYear() >= 2022) {
                throw new IllegalArgumentException("Feil i løsningen. Har tilkjent utbetaling til bruker uten søknad. Gjelder en eller flere aktiviteter eller arbeidsgivere for periodene " + perioder);
            } else {
                logger.warn("Har tilkjent utbetaling til bruker uten søknad. Gjelder en eller flere aktiviteter eller arbeidsgivere for periodene {}", perioder);
            }
        }
    }

    private LocalDateTimeline<Set<Arbeidsgiver>> finnUtbetalingUtenKrav(LocalDateTimeline<Set<Arbeidsgiver>> krav, LocalDateTimeline<Set<Arbeidsgiver>> utbetalinger) {
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingUtenKrav = utbetalinger.combine(krav, TidsserieUtil::minus, LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return utbetalingUtenKrav
            .filterValue(v -> !v.isEmpty())
            .compress();
    }

    private LocalDateTimeline<Set<Arbeidsgiver>> kravTidslinje(List<OppgittFraværPeriode> alleKravperioder) {
        return new LocalDateTimeline<>(alleKravperioder.stream()
            .map(this::mapPeriode).toList(), TidsserieUtil::union);
    }

    private LocalDateTimeline<Set<Arbeidsgiver>> tidslinjeUtbetaling(BeregningsresultatEntitet beregningsresultat, boolean tilBruker) {
        return new LocalDateTimeline<>(beregningsresultat.getBeregningsresultatPerioder().stream().map(p -> mapPeriode(p, tilBruker)).toList(), TidsserieUtil::union);
    }

    private LocalDateSegment<Set<Arbeidsgiver>> mapPeriode(BeregningsresultatPeriode brPeriode, boolean tilBruker) {
        Set<Arbeidsgiver> tilBrukerMedAndelPrArbeidsgiver = brPeriode.getBeregningsresultatAndelList().stream()
            .filter(a -> a.erBrukerMottaker() == tilBruker && a.getDagsats() > 0)
            .map(this::mapArbeidsgiver)
            .collect(Collectors.toSet());
        return new LocalDateSegment<>(brPeriode.getPeriode().getFomDato(), brPeriode.getPeriode().getTomDato(), tilBrukerMedAndelPrArbeidsgiver);
    }

    private Arbeidsgiver mapArbeidsgiver(BeregningsresultatAndel a) {
        return a.getArbeidsgiver().orElse(null);
    }


    private LocalDateSegment<Set<Arbeidsgiver>> mapPeriode(OppgittFraværPeriode periode) {
        Set<Arbeidsgiver> arbeidsgivere = new HashSet<>();
        arbeidsgivere.add(mapArbeidsgiver(periode));
        return new LocalDateSegment<>(periode.getFom(), periode.getTom(), arbeidsgivere);
    }

    private Arbeidsgiver mapArbeidsgiver(OppgittFraværPeriode periode) {
        return periode.getArbeidsgiver();
    }
}
