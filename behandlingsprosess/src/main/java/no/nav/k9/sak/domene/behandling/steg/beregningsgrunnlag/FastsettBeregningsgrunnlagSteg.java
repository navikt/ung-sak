package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningStegTjeneste.FortsettBeregningResultatCallback;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FAST_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;

    protected FastsettBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                          BeregningStegTjeneste beregningStegTjeneste,
                                          BeregningsgrunnlagVilkårTjeneste vilkårTjeneste) {

        this.beregningStegTjeneste = beregningStegTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        class HåndterResultat implements FortsettBeregningResultatCallback {
            @Override
            public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
                if (kalkulusResultat.getVilkårOppfylt() != null && !kalkulusResultat.getVilkårOppfylt()) {
                    vilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, periode, kalkulusResultat.getAvslagsårsak());
                }
            }
        }

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var callback = new HåndterResultat();
        beregningStegTjeneste.fortsettBeregning(ref, FASTSETT_BEREGNINGSGRUNNLAG, callback);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }
}
