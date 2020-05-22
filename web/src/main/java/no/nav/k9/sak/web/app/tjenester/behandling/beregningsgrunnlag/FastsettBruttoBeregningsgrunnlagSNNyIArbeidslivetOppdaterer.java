package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer implements AksjonspunktOppdaterer<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto>{


    private BeregningTjeneste kalkulusTjeneste;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer(BeregningTjeneste kalkulusTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto, AksjonspunktOppdaterParameter param) {
        HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.map(dto);
        kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, param.getRef(), dto.getSkjæringstidspunkt());
        return OppdateringResultat.utenOveropp();

    }
}
