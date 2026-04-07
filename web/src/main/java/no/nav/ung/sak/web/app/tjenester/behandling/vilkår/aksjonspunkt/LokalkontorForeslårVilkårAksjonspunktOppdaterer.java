package no.nav.ung.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.domene.vedtak.OppdaterAnsvarligSaksbehandlerTjeneste;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.LokalkontorForeslåVilkårAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakOppdatererTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.OpprettToTrinnsgrunnlag;

import java.util.Set;

@ApplicationScoped
@DtoTilServiceAdapter(dto = LokalkontorForeslåVilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class LokalkontorForeslårVilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<LokalkontorForeslåVilkårAksjonspunktDto> {

    private Instance<OppdaterAnsvarligSaksbehandlerTjeneste> oppdaterAnsvarligSaksbehandlerTjenester;

    private OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;

    LokalkontorForeslårVilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public LokalkontorForeslårVilkårAksjonspunktOppdaterer(OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                                           @Any Instance<OppdaterAnsvarligSaksbehandlerTjeneste> oppdaterAnsvarligSaksbehandlerTjenester) {
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.oppdaterAnsvarligSaksbehandlerTjenester = oppdaterAnsvarligSaksbehandlerTjenester;
    }

    @Override
    public OppdateringResultat oppdater(LokalkontorForeslåVilkårAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        OppdaterAnsvarligSaksbehandlerTjeneste oppdaterAnsvarligSaksbehandlerTjeneste = OppdaterAnsvarligSaksbehandlerTjeneste.finnTjeneste(oppdaterAnsvarligSaksbehandlerTjenester, param.getRef().getFagsakYtelseType());
        oppdaterAnsvarligSaksbehandlerTjeneste.oppdaterAnsvarligSaksbehandler(Set.of(dto), param.getBehandlingId());

        OppdateringResultat.Builder builder = OppdateringResultat.builder();

        opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(param.getBehandling());


        return builder.build();
    }

}
