package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNHåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBruttoBeregningsgrunnlagSNDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBruttoBeregningsgrunnlagSNDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBruttoBeregningsgrunnlagSNOppdaterer implements AksjonspunktOppdaterer<FastsettBruttoBeregningsgrunnlagSNDto>{

    private FastsettBruttoBeregningsgrunnlagSNHåndterer fastsettBruttoBeregningsgrunnlagSNHåndterer;
    private FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste;

    FastsettBruttoBeregningsgrunnlagSNOppdaterer() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNOppdaterer(FastsettBruttoBeregningsgrunnlagSNHåndterer fastsettBruttoBeregningsgrunnlagSNHåndterer,
                                                        FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste) {
        this.fastsettBruttoBeregningsgrunnlagSNHåndterer = fastsettBruttoBeregningsgrunnlagSNHåndterer;
        this.fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste = fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBruttoBeregningsgrunnlagSNDto dto, AksjonspunktOppdaterParameter param) {
        fastsettBruttoBeregningsgrunnlagSNHåndterer.håndter(param.getBehandlingId(), dto);
        fastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste.lagHistorikk(param, dto);
        return OppdateringResultat.utenOveropp();
    }
}
