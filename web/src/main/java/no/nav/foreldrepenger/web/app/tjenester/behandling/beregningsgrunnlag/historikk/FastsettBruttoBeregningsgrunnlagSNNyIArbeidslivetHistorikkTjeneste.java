package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

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
