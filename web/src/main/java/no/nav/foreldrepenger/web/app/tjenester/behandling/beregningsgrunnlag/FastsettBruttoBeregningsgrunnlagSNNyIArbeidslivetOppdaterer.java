package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer implements AksjonspunktOppdaterer<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto>{


    private FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer;
    private FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetOppdaterer(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer,
                                                                       FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste) {
        this.fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer = fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer;
        this.fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste = fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto, AksjonspunktOppdaterParameter param) {

        fastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer.oppdater(param.getBehandlingId(), dto);
        fastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste.lagHistorikk(dto, param);

        return OppdateringResultat.utenOveropp();

    }
}
