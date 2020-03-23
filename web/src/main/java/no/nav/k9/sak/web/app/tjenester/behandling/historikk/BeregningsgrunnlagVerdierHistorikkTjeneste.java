package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagPeriodeEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

/**
 * Lager historikk for endret inntekt og inntektskategori etter oppdatering fra Kalkulus.
 */
@ApplicationScoped
public class BeregningsgrunnlagVerdierHistorikkTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private InntektHistorikkTjeneste inntektHistorikkTjeneste;
    private InntektskategoriHistorikkTjeneste inntektskategoriHistorikkTjeneste;

    public BeregningsgrunnlagVerdierHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public BeregningsgrunnlagVerdierHistorikkTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                      InntektHistorikkTjeneste inntektHistorikkTjeneste,
                                                      InntektskategoriHistorikkTjeneste inntektskategoriHistorikkTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.inntektHistorikkTjeneste = inntektHistorikkTjeneste;
        this.inntektskategoriHistorikkTjeneste = inntektskategoriHistorikkTjeneste;
    }

    public void lagHistorikkForBeregningsgrunnlagVerdier(Long behandlingId, BeregningsgrunnlagPeriodeEndring periode, HistorikkInnslagTekstBuilder tekstBuilder) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        periode.getBeregningsgrunnlagPrStatusOgAndelEndringer()
            .forEach(andelEndring -> lagHistorikkForAndel(tekstBuilder, arbeidsforholdOverstyringer, andelEndring));
    }

    private void lagHistorikkForAndel(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        inntektHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, arbeidsforholdOverstyringer, andelEndring);
        inntektskategoriHistorikkTjeneste.lagHistorikkOmEndret(tekstBuilder, arbeidsforholdOverstyringer, andelEndring);
    }

}
