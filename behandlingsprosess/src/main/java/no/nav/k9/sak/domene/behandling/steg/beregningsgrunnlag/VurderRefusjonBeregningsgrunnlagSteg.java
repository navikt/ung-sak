package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_REF_BERGRUNN;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "VURDER_REF_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;

    protected VurderRefusjonBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                                BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                                BeregningStegTjeneste beregningStegTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningStegTjeneste = beregningStegTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        class HåndterResultat implements FortsettBeregningResultatCallback {

            @Override
            public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
                if (kalkulusResultat.getVilkårOppfylt() != null && !kalkulusResultat.getVilkårOppfylt()) {
                    beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, periode, kalkulusResultat.getAvslagsårsak());
                } else if (kalkulusResultat.getVilkårOppfylt() != null) {
                    beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, periode, kalkulusResultat.getVilkårOppfylt());
                }
            }
        }

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);

        var callback = new HåndterResultat();
        beregningStegTjeneste.fortsettBeregning(ref, VURDER_REF_BERGRUNN, callback);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!tilSteg.equals(VURDER_REF_BERGRUNN)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ref = BehandlingReferanse.fra(behandling);
            beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, false)
                .forEach(periode -> beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, periode));
        }
    }

}
