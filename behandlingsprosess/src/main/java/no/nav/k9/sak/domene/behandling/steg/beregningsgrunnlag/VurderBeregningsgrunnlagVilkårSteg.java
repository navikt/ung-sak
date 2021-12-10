package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_VILKAR_BERGRUNN;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningStegTjeneste.FortsettBeregningResultatCallback;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "VURDER_VILKAR_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class VurderBeregningsgrunnlagVilkårSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;

    protected VurderBeregningsgrunnlagVilkårSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderBeregningsgrunnlagVilkårSteg(BehandlingRepository behandlingRepository,
                                              BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                              BeregningStegTjeneste beregningStegTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningStegTjeneste = beregningStegTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var callback = new HåndterResultat(kontekst);
        beregningStegTjeneste.fortsettBeregning(ref, VURDER_VILKAR_BERGRUNN, callback);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(callback.aksjonspunktResultater);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!tilSteg.equals(VURDER_VILKAR_BERGRUNN)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ref = BehandlingReferanse.fra(behandling);
            beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, false)
                .forEach(periode -> beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, periode));
        }
    }

    class HåndterResultat implements FortsettBeregningResultatCallback {

        private List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();
        private BehandlingskontrollKontekst kontekst;

        public HåndterResultat(BehandlingskontrollKontekst kontekst) {
            this.kontekst = kontekst;
        }

        @Override
        public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
            if (kalkulusResultat.getVilkårOppfylt() != null && !kalkulusResultat.getVilkårOppfylt()) {
                beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, periode, kalkulusResultat.getAvslagsårsak());
            } else if (kalkulusResultat.getVilkårOppfylt() != null) {
                Avslagsårsak avslagsårsak = kalkulusResultat.getVilkårOppfylt() ? null : Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG;
                beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, periode, avslagsårsak);
            }
            aksjonspunktResultater.addAll(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

}
