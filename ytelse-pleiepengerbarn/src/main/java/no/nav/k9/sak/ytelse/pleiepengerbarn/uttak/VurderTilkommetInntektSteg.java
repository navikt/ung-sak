package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_TILKOMMET_INNTEKT;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningResultatMapper;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningStegTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_TILKOMMET_INNTEKT)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class VurderTilkommetInntektSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private VilkårTjeneste vilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;
    private boolean skalKjøreSteget;

    protected VurderTilkommetInntektSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderTilkommetInntektSteg(BehandlingRepository behandlingRepository,
                                      BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                      VilkårTjeneste vilkårTjeneste, BeregningStegTjeneste beregningStegTjeneste,
                                      @KonfigVerdi(value = "TILKOMMET_AKTIVITET_ENABLED", defaultVerdi = "false") boolean skalKjøreSteget) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.beregningStegTjeneste = beregningStegTjeneste;
        this.skalKjøreSteget = skalKjøreSteget;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!skalKjøreSteget) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        validerVurdertVilkår(ref);
        var callback = new VurderTilkommetInntektSteg.HåndterResultat();
        beregningStegTjeneste.fortsettBeregningInkludertForlengelser(ref, VURDER_TILKOMMET_INNTEKT, callback);
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

    class HåndterResultat implements BeregningStegTjeneste.FortsettBeregningResultatCallback {
        private List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        @Override
        public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
            aksjonspunktResultater.addAll(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

}
