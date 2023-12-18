package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG;

import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
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
import no.nav.k9.sak.ytelse.beregning.OppdaterKvoteTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = FASTSETT_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;

    private Instance<OppdaterKvoteTjeneste> oppdaterKvoteTjenester;

    protected FastsettBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                          BeregningStegTjeneste beregningStegTjeneste,
                                          BeregningsgrunnlagVilkårTjeneste vilkårTjeneste,
                                          @Any Instance<OppdaterKvoteTjeneste> oppdaterKvoteTjenester) {

        this.beregningStegTjeneste = beregningStegTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.oppdaterKvoteTjenester = oppdaterKvoteTjenester;
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
        beregningStegTjeneste.fortsettBeregningInkludertForlengelser(ref, FASTSETT_BEREGNINGSGRUNNLAG, callback);


        OppdaterKvoteTjeneste.finnTjeneste(oppdaterKvoteTjenester, ref.getFagsakYtelseType()).oppdaterKvote(ref);


        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }
}
