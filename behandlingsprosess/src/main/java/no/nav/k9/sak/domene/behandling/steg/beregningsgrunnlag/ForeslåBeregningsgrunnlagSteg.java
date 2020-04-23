package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FORS_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private Instance<KalkulusTjeneste> kalkulusTjeneste;

    protected ForeslåBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                         @Any Instance<KalkulusTjeneste> kalkulusTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);

        List<AksjonspunktResultat> aksjonspunkter = FagsakYtelseTypeRef.Lookup.find(kalkulusTjeneste, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste"))
            .fortsettBeregning(ref, FORESLÅ_BEREGNINGSGRUNNLAG)
            .getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList());
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

}
