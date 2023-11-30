package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_TILKOMMET_INNTEKT;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
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
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private final BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private final BeregningsgrunnlagTjeneste kalkulusTjeneste;
    private final BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private final KalkulusStartpunktUtleder kalkulusStartpunktUtleder;


    @Inject
    public KopierBeregningTjeneste(BehandlingRepository behandlingRepository, @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                   BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                   BeregningsgrunnlagTjeneste kalkulusTjeneste,
                                   BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                   KalkulusStartpunktUtleder kalkulusStartpunktUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusStartpunktUtleder = kalkulusStartpunktUtleder;
    }

    public void kopierVurderinger(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        // 1. Kopierer grunnlag og vilkårsresultat for forlengelser
        kopierGrunnlagForForlengelseperioder(referanse);

        // 2. kopierer input overstyringer for migrering fra infotrygd
        kopierInputOverstyring(behandling);

    }


    /**
     * Kopierer overstyrt input i revurderinger som ikke er manuelt opprettet (se https://jira.adeo.no/browse/TSF-2658)
     *
     * @param behandling Behandling
     */
    private void kopierInputOverstyring(Behandling behandling) {
        var perioderTilVurdering = vurdertePerioder(BehandlingReferanse.fra(behandling));
        if (behandling.erRevurdering() && !behandling.erManueltOpprettet() && !harEksisterendeOverstyringer(behandling, perioderTilVurdering)) {
            var kopiertInputOverstyring = behandling.getOriginalBehandlingId().flatMap(beregningPerioderGrunnlagRepository::hentGrunnlag)
                .stream()
                .flatMap(it -> it.getInputOverstyringPerioder().stream())
                .filter(it -> perioderTilVurdering.stream().anyMatch(p -> p.getFomDato().equals(it.getSkjæringstidspunkt())))
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


    private Set<DatoIntervallEntitet> vurdertePerioder(BehandlingReferanse ref) {
        var tjeneste = getPerioderTilVurderingTjeneste(ref);
        return tjeneste.utled(ref.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }

    /**
     * Kopierer grunnlag og vilkårsresultat for forlengelser
     *
     * @param ref behandlingreferanse
     */
    private void kopierGrunnlagForForlengelseperioder(BehandlingReferanse ref) {
        var perioderPrStartpunkt = kalkulusStartpunktUtleder.utledPerioderPrStartpunkt(ref);
        if (ref.getBehandlingType().equals(BehandlingType.REVURDERING)) {
            var startpunktVurderRefusjon = perioderPrStartpunkt.get(VURDER_TILKOMMET_INNTEKT);
            if (!startpunktVurderRefusjon.isEmpty()) {
                log.info("Kopierer beregning for startpunkt vurder refusjon {}", startpunktVurderRefusjon);
                kalkulusTjeneste.kopier(ref, startpunktVurderRefusjon, StegType.VURDER_VILKAR_BERGRUNN);
                var originalBehandlingId = ref.getOriginalBehandlingId().orElseThrow();
                beregningsgrunnlagVilkårTjeneste.kopierVilkårresultatFraForrigeBehandling(
                    ref.getBehandlingId(),
                    originalBehandlingId,
                    startpunktVurderRefusjon.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet()));
            }
            var startpunktKontrollerFakta = perioderPrStartpunkt.get(KONTROLLER_FAKTA_BEREGNING);
            if (!startpunktKontrollerFakta.isEmpty()) {
                log.info("Kopierer beregning for startpunkt kontroller fakta {}", startpunktKontrollerFakta);
                kalkulusTjeneste.kopier(ref, startpunktKontrollerFakta, StegType.FASTSETT_STP_BER);
            }
        }
    }


}
