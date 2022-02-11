package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.SøknadPerioderTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.KravDokumentFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class OmsorgspengerYtelseVerifiserer {

    private SøknadPerioderTjeneste søknadPerioderTjeneste;
    private VurderSøknadsfristTjeneste<OppgittFraværPeriode> søknadsfristTjeneste;

    @Inject
    public OmsorgspengerYtelseVerifiserer(SøknadPerioderTjeneste søknadPerioderTjeneste,
                                          @FagsakYtelseTypeRef("OMP") VurderSøknadsfristTjeneste<OppgittFraværPeriode> søknadsfristTjeneste) {
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public void verifiser(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);
        verifiserUtbetalingTilBrukerKunHvorSøknad(behandling, beregningsresultat);
    }

    private void verifiserUtbetalingTilBrukerKunHvorSøknad(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        var søkteFraværsperioder = søknadPerioderTjeneste.hentSøktePerioderMedKravdokumentPåFagsak(behandling.getFagsakId());
        var søkteFraværsperioderSøknadsfristbehandlet = søknadsfristTjeneste.vurderSøknadsfrist(behandling.getId(), søkteFraværsperioder);
        var søkteFraværesperioderUtenOverlapp = new KravDokumentFravær().trekkUtAlleFraværOgValiderOverlapp(søkteFraværsperioderSøknadsfristbehandlet);

        LocalDateTimeline<Set<Arbeidsgiver>> søknadTidslinje = new LocalDateTimeline<>(søkteFraværesperioderUtenOverlapp.stream().map(this::mapPeriode).toList(), TidsserieUtil::union);
        LocalDateTimeline<Set<Arbeidsgiver>> utbetalingTilSøkerTidslinje = new LocalDateTimeline<>(beregningsresultat.getBeregningsresultatPerioder().stream().map(this::mapPeriode).toList(), TidsserieUtil::union);

        LocalDateTimeline<Set<Arbeidsgiver>> tidslinjeManglerSøknad = utbetalingTilSøkerTidslinje.combine(søknadTidslinje, TidsserieUtil::minus, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .filterValue(v -> !v.isEmpty())
            .compress();

        if (!tidslinjeManglerSøknad.isEmpty()) {
            List<Periode> perioder = tidslinjeManglerSøknad.stream().map(segment -> new Periode(segment.getFom(), segment.getTom())).toList();
            throw new IllegalArgumentException("Har utbetaling til bruker uten søknad. Gjelder en eller flere aktiviteter for periodene " + perioder);
        }
    }

    private LocalDateSegment<Set<Arbeidsgiver>> mapPeriode(BeregningsresultatPeriode brPeriode) {
        Set<Arbeidsgiver> tilBrukerMedAndelPrArbeidsgiver = brPeriode.getBeregningsresultatAndelList().stream()
            .filter(a -> a.erBrukerMottaker() && a.getDagsats() > 0)
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
