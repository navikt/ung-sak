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
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDtoer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDtoer.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer implements AksjonspunktOppdaterer<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDtoer> {

    private BeregningTjeneste kalkulusTjeneste;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer(BeregningTjeneste kalkulusTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDtoer dtoer, AksjonspunktOppdaterParameter param) {
        Map<LocalDate, HåndterBeregningDto> stpTilDtoMap = dtoer.getGrunnlag().stream()
            .collect(Collectors.toMap(dto -> dto.getPeriode().getFom(), MapDtoTilRequest::map));
        kalkulusTjeneste.oppdaterBeregningListe(stpTilDtoMap, param.getRef());
        // TODO FIKS HISTORIKK
        return OppdateringResultat.utenOveropp();

    }
}
