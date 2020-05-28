package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FordelBeregningsgrunnlagDtoer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FordelBeregningsgrunnlagDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class FordelBeregningsgrunnlagOppdaterer implements AksjonspunktOppdaterer<FordelBeregningsgrunnlagDtoer> {

    private BeregningTjeneste kalkulusTjeneste;


    FordelBeregningsgrunnlagOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagOppdaterer(BeregningTjeneste kalkulusTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FordelBeregningsgrunnlagDtoer dtoer, AksjonspunktOppdaterParameter param) {
        for (FordelBeregningsgrunnlagDto dto : dtoer.getGrunnlag()) {
            HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.map(dto);
            var resultat = kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, param.getRef(), dto.getSkjæringstidspunkt());
        }
        return OppdateringResultat.utenOveropp();
    }

}
