package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;

@ApplicationScoped
public class FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;

    FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNNyIArbeidslivetHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    public void lagHistorikk(FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto, AksjonspunktOppdaterParameter param) {
        oppdaterVedEndretVerdi(dto.getBruttoBeregningsgrunnlag());
        boolean erBegrunnelseEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), erBegrunnelseEndret)
            .medSkjermlenke(SkjermlenkeType.BEREGNING);
    }

    private void oppdaterVedEndretVerdi(Integer bruttoNæringsInntekt) {
        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.BRUTTO_NAERINGSINNTEKT, null, bruttoNæringsInntekt);
    }

}
