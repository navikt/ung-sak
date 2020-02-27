package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.AvklarteAktiviteterDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarteAktiviteterDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktiviteterOppdaterer implements AksjonspunktOppdaterer<AvklarteAktiviteterDto> {

    private KalkulusTjeneste kalkulusTjeneste;

    AvklarAktiviteterOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktiviteterOppdaterer(KalkulusTjeneste kalkulusTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarteAktiviteterDto dto, AksjonspunktOppdaterParameter param) {
        HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.map(dto);
        List<BeregningAksjonspunktResultat> resultat = kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, param.getRef());
        // TODO Ta i bruk respons for historikk når dette er klart på kalkulus
        return OppdateringResultat.utenOveropp();
    }

}
