package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_REF_BERGRUNN;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
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
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = VURDER_REF_BERGRUNN)
@BehandlingTypeRef
@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private VilkårTjeneste vilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;

    protected VurderRefusjonBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                                BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                                VilkårTjeneste vilkårTjeneste, BeregningStegTjeneste beregningStegTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.beregningStegTjeneste = beregningStegTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        validerVurdertVilkår(ref);
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

    class HåndterResultat implements FortsettBeregningResultatCallback {
        private List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        @Override
        public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
            aksjonspunktResultater.addAll(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

}
