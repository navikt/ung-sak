package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBruttoBeregningsgrunnlagSNDto;

@ApplicationScoped
public class FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;

     FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste() {
        // CDI
     }

    @Inject
    public FastsettBruttoBeregningsgrunnlagSNHistorikkTjeneste(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    public void lagHistorikk(AksjonspunktOppdaterParameter param, FastsettBruttoBeregningsgrunnlagSNDto dto) {
        HistorikkInnslagTekstBuilder historikkDelBuilder = historikkAdapter.tekstBuilder();
        historikkDelBuilder.ferdigstillHistorikkinnslagDel();
        oppdaterVedEndretVerdi(historikkDelBuilder, dto.getBruttoBeregningsgrunnlag());

        boolean erBegrunnelseEndret = param.erBegrunnelseEndret();
        historikkDelBuilder.medBegrunnelse(dto.getBegrunnelse(), erBegrunnelseEndret);
    }

    private void oppdaterVedEndretVerdi(HistorikkInnslagTekstBuilder historikkDelBuilder, Integer bruttoNæringsInntekt) {
        historikkDelBuilder.medEndretFelt(HistorikkEndretFeltType.BRUTTO_NAERINGSINNTEKT, null, bruttoNæringsInntekt);
    }

}
