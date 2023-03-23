package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.PRECONDITION_BEREGNING;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = PRECONDITION_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class VurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere;
    private VurderAvslagGrunnetOpptjening vurderAvslagGrunnetOpptjening;
    private RyddOgGjenopprettBeregningTjeneste ryddOgGjenopprettBeregningTjeneste;
    private KopierBeregningTjeneste kopierBeregningTjeneste;


    protected VurderPreconditionBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderPreconditionBeregningSteg(BehandlingRepository behandlingRepository,
                                           @Any Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere,
                                           VurderAvslagGrunnetOpptjening vurderAvslagGrunnetOpptjening,
                                           RyddOgGjenopprettBeregningTjeneste ryddOgGjenopprettBeregningTjeneste,
                                           KopierBeregningTjeneste kopierBeregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.aksjonspunktUtledere = aksjonspunktUtledere;
        this.vurderAvslagGrunnetOpptjening = vurderAvslagGrunnetOpptjening;
        this.ryddOgGjenopprettBeregningTjeneste = ryddOgGjenopprettBeregningTjeneste;
        this.kopierBeregningTjeneste = kopierBeregningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        // 1. Rydder
        ryddOgGjenopprettBeregningTjeneste.ryddOgGjenopprett(kontekst);

        // 2. Avslå der opptjening er avslått
        vurderAvslagGrunnetOpptjening.vurderAvslagGrunnetAvslagIOpptjening(referanse);

        // 3. Rydder fjernet eller avslått periode (må vurdere avslag mellom dei to rydde-kalla)
        ryddOgGjenopprettBeregningTjeneste.deaktiverAvslåtteEllerFjernetPerioder(referanse);

        // 4. Kopier
        kopierBeregningTjeneste.kopierVurderinger(kontekst);

        // 5. Utled aksjonspunkt
        var aksjonspunkter = finnAksjonspunkter(referanse);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }


    private List<AksjonspunktResultat> finnAksjonspunkter(BehandlingReferanse behandlingReferanse) {
        FagsakYtelseType ytelseType = behandlingReferanse.getFagsakYtelseType();

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(aksjonspunktUtledere, ytelseType);
        return tjeneste.map(utleder -> utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(behandlingReferanse)))
            .orElse(Collections.emptyList());
    }


}
