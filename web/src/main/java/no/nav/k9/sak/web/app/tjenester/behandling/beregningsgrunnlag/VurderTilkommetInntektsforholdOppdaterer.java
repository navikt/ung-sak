package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.FordelBeregningsgrunnlagDtoer;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.fordeling.VurderTilkomneInntektsforholdDtoer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderTilkomneInntektsforholdDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class VurderTilkommetInntektsforholdOppdaterer implements AksjonspunktOppdaterer<VurderTilkomneInntektsforholdDtoer> {

    private BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste;


    VurderTilkommetInntektsforholdOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderTilkommetInntektsforholdOppdaterer(BeregningsgrunnlagOppdateringTjeneste oppdateringTjeneste) {
        this.oppdateringTjeneste = oppdateringTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderTilkomneInntektsforholdDtoer dtoer, AksjonspunktOppdaterParameter param) {
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), dto1 -> MapDtoTilRequest.map(dto1, dtoer.getBegrunnelse())));
        oppdateringTjeneste.oppdaterBeregning(stpTilDtoMap, param.getRef(), true);
        // TODO FIKS HISTORIKK
        return OppdateringResultat.nyttResultat();
    }

}
