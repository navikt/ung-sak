package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.InntektskategoriEndring;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

/**
 * Historikktjeneste for endring av inntektskategori
 */
@Dependent
class InntektskategoriHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    public InntektskategoriHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public InntektskategoriHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    void lagHistorikkOmEndret(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        Optional<InntektskategoriEndring> inntektskategoriEndring = andelEndring.getInntektskategoriEndring();
        inntektskategoriEndring.ifPresent(endring -> tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKTSKATEGORI,
            arbeidsgiverHistorikkinnslag.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                andelEndring.getAktivitetStatus(),
                andelEndring.getArbeidsgiver(),
                Optional.of(andelEndring.getArbeidsforholdRef()),
                arbeidsforholdOverstyringer),
            endring.getFraVerdi(),
            endring.getTilVerdi()));
    }
}
