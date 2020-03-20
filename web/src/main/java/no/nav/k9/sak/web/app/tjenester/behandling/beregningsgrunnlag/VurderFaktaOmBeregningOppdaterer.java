package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderFaktaOmBeregningDto;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.FaktaOmBeregningHistorikkTjeneste;


@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBeregningDto> {

    private KalkulusTjeneste kalkulusTjeneste;
    private FaktaOmBeregningHistorikkTjeneste faktaOmBeregningHistorikkTjeneste;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(KalkulusTjeneste kalkulusTjeneste, FaktaOmBeregningHistorikkTjeneste faktaOmBeregningHistorikkTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.faktaOmBeregningHistorikkTjeneste = faktaOmBeregningHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDto dto, AksjonspunktOppdaterParameter param) {
        HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.map(dto);
        var resultat = kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, param.getRef());
        faktaOmBeregningHistorikkTjeneste.lagHistorikk(param.getBehandlingId(), resultat, dto.getBegrunnelse());
        return OppdateringResultat.utenOveropp();
    }

}
