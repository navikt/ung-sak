package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;

/**
 * Tjeneste som setter opp tidslinjen som brukes til å sammenligne beregningsresultatet mellom originalbehandlingen og revurderingen.
 * Brukes for å sjekke om søker har mistet penger til arbeidsgiver i løpet av beregningsresultatet mellom behandlingene.
 */
@ApplicationScoped
public class BeregningsresultatTidslinjetjeneste {
    BeregningsresultatRepository beregningsresultatRepository;

    protected BeregningsresultatTidslinjetjeneste() {
        // CDI
    }

    @Inject
    public BeregningsresultatTidslinjetjeneste(BeregningsresultatRepository beregningsresultatRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    /**
     * @param ref revurderingens behandlingreferanse
     * @return tidslinje over revurderingen og originalbehandlingens beregningsresultat
     */
    public LocalDateTimeline<BRAndelSammenligning> lagTidslinjeForRevurdering(BehandlingReferanse ref) {
        verifiserAtBehandlingIkkeErFørstegangsbehandling(ref);
        Long behandlingId = ref.getBehandlingId();

        // Nytt resultat, her aksepterer vi ikke tomt resultat siden vi har kommet til steget der vi vurderer beregningsresultatet.
        BeregningsresultatEntitet revurderingBeregningsresultat = beregningsresultatRepository.hentBgBeregningsresultat(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        // Gammelt resultat, kan være tomt (f.eks ved avslått)
        Optional<BeregningsresultatEntitet> originaltBeregningsresultat = ref.getOriginalBehandlingId().flatMap(beregningsresultatRepository::hentEndeligBeregningsresultat);

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

}
