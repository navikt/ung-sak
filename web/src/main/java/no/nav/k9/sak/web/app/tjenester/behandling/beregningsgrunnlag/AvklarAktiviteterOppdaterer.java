package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;


import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.AvklarteAktiviteterDtoer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarteAktiviteterDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktiviteterOppdaterer implements AksjonspunktOppdaterer<AvklarteAktiviteterDtoer> {

    private BeregningTjeneste kalkulusTjeneste;

    AvklarAktiviteterOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktiviteterOppdaterer(BeregningTjeneste kalkulusTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarteAktiviteterDtoer dtoer, AksjonspunktOppdaterParameter param) {
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), MapDtoTilRequest::map));
        kalkulusTjeneste.oppdaterBeregningListe(stpTilDtoMap, param.getRef());
        return OppdateringResultat.utenOveropp();
    }

}
