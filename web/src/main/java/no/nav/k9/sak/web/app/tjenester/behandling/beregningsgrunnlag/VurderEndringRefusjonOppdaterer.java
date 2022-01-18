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
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.refusjon.VurderRefusjonBeregningsgrunnlagDtoer;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning.VurderRefusjonBeregningsgrunnlagHistorikkTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderRefusjonBeregningsgrunnlagDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class VurderEndringRefusjonOppdaterer implements AksjonspunktOppdaterer<VurderRefusjonBeregningsgrunnlagDtoer> {

    private BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste;
    private VurderRefusjonBeregningsgrunnlagHistorikkTjeneste historikkTjeneste;


    VurderEndringRefusjonOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderEndringRefusjonOppdaterer(BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste, VurderRefusjonBeregningsgrunnlagHistorikkTjeneste historikkTjeneste) {
        this.oppdateringTjeneste = oppdateringTjeneste;
        this.historikkTjeneste = historikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderRefusjonBeregningsgrunnlagDtoer dtoer, AksjonspunktOppdaterParameter param) {
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), dto1 -> MapDtoTilRequest.map(dto1, dtoer.getBegrunnelse())));
        var oppdaterResultat = oppdateringTjeneste.oppdaterBeregning(stpTilDtoMap, param.getRef());
        historikkTjeneste.lagHistorikk(param, dtoer, oppdaterResultat);
        return OppdateringResultat.utenOverhopp();
    }

}
