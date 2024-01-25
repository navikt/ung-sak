package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagReferanserTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningStegTjeneste.FortsettBeregningResultatCallback;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_REF_BERGRUNN;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = VURDER_REF_BERGRUNN)
@BehandlingTypeRef
@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private VilkårTjeneste vilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;
    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private BeregningsgrunnlagReferanserTjeneste referanserTjeneste;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    protected VurderRefusjonBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                                BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                                VilkårTjeneste vilkårTjeneste, BeregningStegTjeneste beregningStegTjeneste,
                                                VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                BeregningsgrunnlagReferanserTjeneste referanserTjeneste,
                                                BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.beregningStegTjeneste = beregningStegTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.referanserTjeneste = referanserTjeneste;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        validerVurdertVilkår(ref);
        kopierGrunnlagForNyePerioderGrunnetEndretUttak(ref, kontekst);
        var callback = new HåndterResultat();
        beregningStegTjeneste.fortsettBeregningInkludertForlengelser(ref, VURDER_REF_BERGRUNN, callback);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(callback.aksjonspunktResultater);
    }

    private void validerVurdertVilkår(BehandlingReferanse ref) {
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);
        var harIkkeVurderteVilkårTilVurdering = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId())
            .getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> perioderTilVurdering.stream().anyMatch(tv -> tv.overlapper(p.getPeriode())))
            .anyMatch(v -> v.getGjeldendeUtfall().equals(Utfall.IKKE_VURDERT));
        if (harIkkeVurderteVilkårTilVurdering) {
            throw new IllegalStateException("Har vilkårsperiode til vurdering som ikke er vurdert");
        }
    }

    private void kopierGrunnlagForNyePerioderGrunnetEndretUttak(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        if (ref.getBehandlingType().equals(BehandlingType.REVURDERING)) {
            Set<PeriodeTilVurdering> nyePerioder = finnPerioderSomVurderesGrunnetEndretUttak(ref);
            if (!nyePerioder.isEmpty()) {
                beregningsgrunnlagTjeneste.kopier(ref, nyePerioder, BehandlingStegType.VURDER_VILKAR_BERGRUNN);
                var originalBehandlingId = ref.getOriginalBehandlingId().orElseThrow();
                beregningsgrunnlagVilkårTjeneste.kopierVilkårresultatFraForrigeBehandling(
                    kontekst.getBehandlingId(), originalBehandlingId,
                    nyePerioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet()));
            }
        }
    }

    private Set<PeriodeTilVurdering> finnPerioderSomVurderesGrunnetEndretUttak(BehandlingReferanse ref) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref).ignorerAvslåttePerioder()
            .markerEndringIUttak();
        var allePerioder = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);
        var endretUttakPerioder = allePerioder.stream()
            .filter(PeriodeTilVurdering::erForlengelse)
            .filter(PeriodeTilVurdering::erEndringIUttak)
            .collect(Collectors.toSet());
        var skjæringstidspunkter = endretUttakPerioder.stream().map(PeriodeTilVurdering::getSkjæringstidspunkt).collect(Collectors.toSet());
        var endretUttakReferanser = referanserTjeneste.finnBeregningsgrunnlagsReferanseFor(
            ref.getBehandlingId(),
            skjæringstidspunkter,
            false,
            true);
        var nyeStpTilVurdering = endretUttakReferanser.stream()
            .filter(BgRef::erGenerertReferanse) // Generert referanse betyr at eksisterende var lik initiell
            .map(BgRef::getStp)
            .collect(Collectors.toSet());
        return endretUttakPerioder.stream()
            .filter(p -> nyeStpTilVurdering.contains(p.getSkjæringstidspunkt()))
            .collect(Collectors.toSet());
    }

    class HåndterResultat implements FortsettBeregningResultatCallback {
        private List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        @Override
        public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
            aksjonspunktResultater.addAll(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

}
