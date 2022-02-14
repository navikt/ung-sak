package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;

@Dependent
public class OmsorgspengerYtelseVerifiserer {

    private static final Logger logger = LoggerFactory.getLogger(OmsorgspengerYtelseVerifiserer.class);

    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    @Inject
    public OmsorgspengerYtelseVerifiserer(TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
    }

    public void verifiser(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);
        verifiserUtbetalingKunHvorKrav(behandling, beregningsresultat);
    }

    private void verifiserUtbetalingKunHvorKrav(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        var alleKrav = trekkUtFraværTjeneste.fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(behandling);
        verifiserRefusjonKunVedRefusjonskrav(alleKrav, beregningsresultat);
        verifiserUtbetalingSøkerKunVedSøknad(alleKrav, beregningsresultat);
    }

    private void verifiserRefusjonKunVedRefusjonskrav(List<WrappedOppgittFraværPeriode> alleKrav, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<Arbeidsgiver>> imKravTidslinje = kravImTidslinje(alleKrav);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingRefusjonTidslinje = tidslinjeUtbetaling(beregningsresultat, false);
        LocalDateTimeline<Set<Arbeidsgiver>> refusjonUtenRefusjonskrav = utbetalingRefusjonTidslinje.combine(imKravTidslinje, TidsserieUtil::minus, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .filterValue(v -> !v.isEmpty())
            .compress();
        if (!refusjonUtenRefusjonskrav.isEmpty()) {
            List<Periode> perioder = refusjonUtenRefusjonskrav.stream().map(segment -> new Periode(segment.getFom(), segment.getTom())).toList();
            if (perioder.get(0).getFom().getYear() >= 2022) {
                throw new IllegalArgumentException("Feil i løsningen. Har tilkjent refusjon uten refusjonskrav. Gjelder en eller flere arbeidsgivere for periodene " + perioder);
            } else {
                logger.warn("Har tilkjent refusjon uten refusjonskrav. Gjelder en eller flere arbeidsgivere for periodene {}", perioder);
            }
        }
    }

    private void verifiserUtbetalingSøkerKunVedSøknad(List<WrappedOppgittFraværPeriode> alleKrav, BeregningsresultatEntitet beregningsresultat) {
        LocalDateTimeline<Set<Arbeidsgiver>> søknadTidslinje = søknadTidslinje(alleKrav);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingTilSøkerTidslinje = tidslinjeUtbetaling(beregningsresultat, true);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingUtenSøknad = utbetalingTilSøkerTidslinje.combine(søknadTidslinje, TidsserieUtil::minus, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .filterValue(v -> !v.isEmpty())
            .compress();
        if (!utbetalingUtenSøknad.isEmpty()) {
            List<Periode> perioder = utbetalingUtenSøknad.stream().map(segment -> new Periode(segment.getFom(), segment.getTom())).toList();
            if (perioder.get(0).getFom().getYear() >= 2022) {
                throw new IllegalArgumentException("Feil i løsningen. Har tilkjent utbetaling til bruker uten søknad. Gjelder en eller flere aktiviteter eller arbeidsgivere for periodene " + perioder);
            } else {
                logger.warn("Har tilkjent utbetaling til bruker uten søknad. Gjelder en eller flere aktiviteter eller arbeidsgivere for periodene {}", perioder);
            }
        }
    }

    private LocalDateTimeline<Set<Arbeidsgiver>> søknadTidslinje(List<WrappedOppgittFraværPeriode> alleKravperioder) {
        return new LocalDateTimeline<>(alleKravperioder.stream()
            .filter(kp -> kp.getSøknadsfristUtfall() == Utfall.OPPFYLT)
            .filter(kp -> kp.getKravDokumentType() == KravDokumentType.SØKNAD || kp.getPeriode().getSøknadÅrsak() != SøknadÅrsak.UDEFINERT) //HAXX når IM prioriteres over søknad, blir likevel SøknadÅrsak fra søknad satt
            .map(this::mapPeriode).toList(), TidsserieUtil::union);
    }

    private LocalDateTimeline<Set<Arbeidsgiver>> kravImTidslinje(List<WrappedOppgittFraværPeriode> alleKravperioder) {
        return new LocalDateTimeline<>(alleKravperioder.stream()
            .filter(kp -> kp.getSøknadsfristUtfall() == Utfall.OPPFYLT)
            .filter(kp -> kp.getKravDokumentType() == KravDokumentType.INNTEKTSMELDING)
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


    private LocalDateSegment<Set<Arbeidsgiver>> mapPeriode(WrappedOppgittFraværPeriode wrappedPeriode) {
        OppgittFraværPeriode periode = wrappedPeriode.getPeriode();
        return new LocalDateSegment<>(periode.getFom(), periode.getTom(), Set.of(mapArbeidsgiver(wrappedPeriode)));
    }

    private Arbeidsgiver mapArbeidsgiver(WrappedOppgittFraværPeriode wrappedPeriode) {
        OppgittFraværPeriode periode = wrappedPeriode.getPeriode();
        if (periode.getArbeidsforholdRef().getReferanse() != null) {
            throw new IllegalArgumentException("Forventer ikke arbeidsforhold her, skal kun få søknadsperioder her p.t. ikke mulig å opplyse arbeidsforhold i søknad.");
        }
        return periode.getArbeidsgiver();
    }
}
