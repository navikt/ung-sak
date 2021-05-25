package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderFaktaOmBeregningDtoer;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.FaktaOmBeregningHistorikkTjeneste;


@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaktaOmBeregningDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaktaOmBeregningOppdaterer implements AksjonspunktOppdaterer<VurderFaktaOmBeregningDtoer> {

    private BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste;
    private FaktaOmBeregningHistorikkTjeneste faktaOmBeregningHistorikkTjeneste;

    VurderFaktaOmBeregningOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBeregningOppdaterer(BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste,
                                            FaktaOmBeregningHistorikkTjeneste faktaOmBeregningHistorikkTjeneste) {
        this.oppdateringTjeneste = oppdateringTjeneste;
        this.faktaOmBeregningHistorikkTjeneste = faktaOmBeregningHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaktaOmBeregningDtoer dtoer, AksjonspunktOppdaterParameter param) {
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), MapDtoTilRequest::map));
        var resultatListe = oppdateringTjeneste.oppdaterBeregning(stpTilDtoMap, param.getRef());
        faktaOmBeregningHistorikkTjeneste.lagHistorikk(param.getBehandlingId(), resultatListe, dtoer.getBegrunnelse());
        return OppdateringResultat.utenOveropp();
    }

}
