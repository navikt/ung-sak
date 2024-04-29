package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;

import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

/**
 * Kopierer vurderinger som skal beholdes fra forrige behandling
 */
@Dependent
public class KopierBeregningTjeneste {

    private static final Logger log = LoggerFactory.getLogger(KopierBeregningTjeneste.class);
    private final BehandlingRepository behandlingRepository;
    private final BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private final BeregningsgrunnlagTjeneste kalkulusTjeneste;
    private final BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private final KalkulusStartpunktUtleder kalkulusStartpunktUtleder;


    @Inject
    public KopierBeregningTjeneste(BehandlingRepository behandlingRepository,
                                   BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                   BeregningsgrunnlagTjeneste kalkulusTjeneste,
                                   BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                   KalkulusStartpunktUtleder kalkulusStartpunktUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusStartpunktUtleder = kalkulusStartpunktUtleder;
    }

    public void kopierVurderinger(BehandlingskontrollKontekst kontekst, NavigableSet<PeriodeTilVurdering> perioderTilVurdering) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        // 1. Kopierer grunnlag og vilkårsresultat for forlengelser
        kopierGrunnlagForForlengelseperioder(referanse, perioderTilVurdering);

        // 2. kopierer input overstyringer for migrering fra infotrygd
        kopierInputOverstyring(behandling, perioderTilVurdering);

    }


    /**
     * Kopierer overstyrt input i revurderinger som ikke er manuelt opprettet (se https://jira.adeo.no/browse/TSF-2658)
     *
     * @param behandling           Behandling
     * @param perioderTilVurdering Perioder til vurdering
     */
    private void kopierInputOverstyring(Behandling behandling, NavigableSet<PeriodeTilVurdering> perioderTilVurdering) {
        var perioder = perioderTilVurdering.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet());
        if (behandling.erRevurdering() && !behandling.erManueltOpprettet() && !harEksisterendeOverstyringer(behandling, perioder)) {
            var kopiertInputOverstyring = behandling.getOriginalBehandlingId().flatMap(beregningPerioderGrunnlagRepository::hentGrunnlag)
                .stream()
                .flatMap(it -> it.getInputOverstyringPerioder().stream())
                .filter(it -> perioder.stream().anyMatch(p -> p.getFomDato().equals(it.getSkjæringstidspunkt())))
                .map(InputOverstyringPeriode::new)
                .toList();
            if (!kopiertInputOverstyring.isEmpty()) {
                beregningPerioderGrunnlagRepository.lagreInputOverstyringer(behandling.getId(), kopiertInputOverstyring);
            }
        }
    }

    private boolean harEksisterendeOverstyringer(Behandling behandling, Set<DatoIntervallEntitet> perioderTilVurdering) {
        return beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId())
            .stream()
            .flatMap(it -> it.getInputOverstyringPerioder().stream())
            .anyMatch(it -> perioderTilVurdering.stream().anyMatch(p -> p.getFomDato().equals(it.getSkjæringstidspunkt())));
    }

    /**
     * Kopierer grunnlag og vilkårsresultat for forlengelser
     *
     * @param ref                  behandlingreferanse
     * @param perioderTilVurdering Perioder til vurdering
     */
    private void kopierGrunnlagForForlengelseperioder(BehandlingReferanse ref, NavigableSet<PeriodeTilVurdering> perioderTilVurdering) {
        var perioderPrStartpunkt = kalkulusStartpunktUtleder.utledPerioderPrStartpunkt(ref, perioderTilVurdering);
        if (ref.getBehandlingType().equals(BehandlingType.REVURDERING)) {
            var startpunktForlengelse = kalkulusStartpunktUtleder.getStartpunktForlengelse(ref);
            var perioderMedStartpunktForlengelse = perioderPrStartpunkt.get(startpunktForlengelse);
            if (!perioderMedStartpunktForlengelse.isEmpty()) {
                log.info("Kopierer beregning for startpunkt {} og perioder {}", startpunktForlengelse.getKode(), perioderMedStartpunktForlengelse);
                kalkulusTjeneste.kopier(ref, perioderMedStartpunktForlengelse, BehandlingStegType.VURDER_VILKAR_BERGRUNN);
                var originalBehandlingId = ref.getOriginalBehandlingId().orElseThrow();
                beregningsgrunnlagVilkårTjeneste.kopierVilkårresultatFraForrigeBehandling(
                    ref.getBehandlingId(),
                    originalBehandlingId,
                    perioderMedStartpunktForlengelse.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet()));
            }
            var startpunktKontrollerFakta = perioderPrStartpunkt.get(KONTROLLER_FAKTA_BEREGNING);
            if (!startpunktKontrollerFakta.isEmpty()) {
                log.info("Kopierer beregning for startpunkt kontroller fakta {}", startpunktKontrollerFakta);
                kalkulusTjeneste.kopier(ref, startpunktKontrollerFakta, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING);
            }
        }
    }


}
