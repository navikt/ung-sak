package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.ytelse.beregning.beregningsresultat.BeregningsresultatProvider;

/**
 * Tjeneste som setter opp tidslinjen som brukes til å sammenligne beregningsresultatet mellom originalbehandlingen og revurderingen.
 * Brukes for å sjekke om søker har mistet penger til arbeidsgiver i løpet av beregningsresultatet mellom behandlingene.
 */
@ApplicationScoped
public class BeregningsresultatTidslinjetjeneste {
    private Instance<BeregningsresultatProvider> beregningsresultatProvidere;

    protected BeregningsresultatTidslinjetjeneste() {
        // CDI
    }

    @Inject
    public BeregningsresultatTidslinjetjeneste(@Any Instance<BeregningsresultatProvider> beregningsresultatProvidere) {
        this.beregningsresultatProvidere = beregningsresultatProvidere;
    }

    /**
     *
     * @param ref revurderingens behandlingreferanse
     * @return tidslinje over revurderingen og originalbehandlingens beregningsresultat
     */
    public LocalDateTimeline<BRAndelSammenligning> lagTidslinjeForRevurdering(BehandlingReferanse ref) {
        verifiserAtBehandlingIkkeErFørstegangsbehandling(ref);
        Long behandlingId = ref.getBehandlingId();
        BeregningsresultatProvider beregningsresultatProvider = getBeregningsresultatProvider(ref);

        // Nytt resultat, her aksepterer vi ikke tomt resultat siden vi har kommet til steget der vi vurderer beregningsresultatet.
        BeregningsresultatEntitet revurderingBeregningsresultat = beregningsresultatProvider.hentBeregningsresultat(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        // Gammelt resultat, kan være tomt (f.eks ved avslått)
        Optional<BeregningsresultatEntitet> originaltBeregningsresultat = ref.getOriginalBehandlingId().flatMap(beregningsresultatProvider::hentUtbetBeregningsresultat);

        List<BeregningsresultatPeriode> resultatperiodeRevurdering = revurderingBeregningsresultat.getBeregningsresultatPerioder();
        List<BeregningsresultatPeriode> resultatperiodeOriginal = originaltBeregningsresultat.isPresent() ? originaltBeregningsresultat.get().getBeregningsresultatPerioder() : Collections.emptyList();

        return mapTidslinje(resultatperiodeOriginal, resultatperiodeRevurdering);
    }

    private void verifiserAtBehandlingIkkeErFørstegangsbehandling(BehandlingReferanse ref) {
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(ref.getBehandlingType())) {
            throw new IllegalStateException("Kan ikke opprette beregningsresultattidslinje for førstegangsbehandlign");
        }
    }

    private LocalDateTimeline<BRAndelSammenligning> mapTidslinje(List<BeregningsresultatPeriode> originaltBeregningsresultat,
                                                                 List<BeregningsresultatPeriode> revurderingBeregningsresultat) {
        return MapBRAndelSammenligningTidslinje.opprettTidslinje(
            originaltBeregningsresultat,
            revurderingBeregningsresultat,
            LocalDate.now());
    }

    private BeregningsresultatProvider getBeregningsresultatProvider(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(BeregningsresultatProvider.class, beregningsresultatProvidere, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() ->  new UnsupportedOperationException("BeregningsresultatProvider ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }
}
