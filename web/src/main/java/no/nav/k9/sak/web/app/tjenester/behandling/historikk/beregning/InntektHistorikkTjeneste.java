package no.nav.k9.sak.web.app.tjenester.behandling.historikk.beregning;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeløpEndring;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

/**
 * Historikktjeneste for endring av inntekt
 */
@Dependent
class InntektHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    public InntektHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public InntektHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    public void lagHistorikkOmEndret(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        Optional<BeløpEndring> inntektEndring = andelEndring.getInntektEndring();
        inntektEndring.ifPresent(endring -> opprettInntektHistorikk(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, endring));
    }

    private void opprettInntektHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        if (andelEndring.getAktivitetStatus().erArbeidstaker()) {
            opprettHistorikkArbeidstakerInntekt(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, beløpEndring);
        } else if (andelEndring.getAktivitetStatus().erFrilanser()) {
            opprettHistorikkFrilansinntekt(tekstBuilder, beløpEndring);
        } else if (AktivitetStatus.DAGPENGER.equals(andelEndring.getAktivitetStatus())) {
            opprettHistorikkDagpengeinntekt(tekstBuilder, beløpEndring);
        } else {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
                andelEndring.getAktivitetStatus().getNavn(),
                beløpEndring.getFraMånedsbeløp(),
                beløpEndring.getTilMånedsbeløp());
        }
    }

    private void opprettHistorikkArbeidstakerInntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        if (OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            opprettHistorikkEtterlønnSluttpakke(tekstBuilder, beløpEndring);
        } else if (andelEndring.getArbeidsgiver().isPresent()) {
            opprettHistorikkArbeidsinntekt(
                tekstBuilder,
                arbeidsforholdOverstyringer,
                andelEndring,
                beløpEndring);
        }
    }

    private void opprettHistorikkDagpengeinntekt(HistorikkInnslagTekstBuilder tekstBuilder, BeløpEndring beløpEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.DAGPENGER_INNTEKT,
            beløpEndring.getFraMånedsbeløp(),
            beløpEndring.getTilMånedsbeløp());
    }

    private void opprettHistorikkEtterlønnSluttpakke(HistorikkInnslagTekstBuilder tekstBuilder, BeløpEndring beløpEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE,
            beløpEndring.getFraMånedsbeløp(),
            beløpEndring.getTilMånedsbeløp());
    }

    private void opprettHistorikkFrilansinntekt(HistorikkInnslagTekstBuilder tekstBuilder, BeløpEndring beløpEndring) {
        tekstBuilder.medEndretFelt(
            HistorikkEndretFeltType.FRILANS_INNTEKT,
            beløpEndring.getFraMånedsbeløp(),
            beløpEndring.getTilMånedsbeløp());
    }

    private void opprettHistorikkArbeidsinntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, BeløpEndring beløpEndring) {
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(
            andelEndring.getArbeidsgiver().get(),
            andelEndring.getArbeidsforholdRef(),
            arbeidsforholdOverstyringer);
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
            arbeidsforholdInfo,
            beløpEndring.getFraMånedsbeløp(),
            beløpEndring.getTilMånedsbeløp());
    }

}
