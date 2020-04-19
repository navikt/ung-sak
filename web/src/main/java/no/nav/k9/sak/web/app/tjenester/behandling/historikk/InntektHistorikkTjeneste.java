package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.output.InntektEndring;
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
        Optional<InntektEndring> inntektEndring = andelEndring.getInntektEndring();
        inntektEndring.ifPresent(endring -> opprettInntektHistorikk(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, endring));
    }

    private void opprettInntektHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, InntektEndring inntektEndring) {
        if (andelEndring.getAktivitetStatus().erArbeidstaker()) {
            opprettHistorikkArbeidstakerInntekt(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, inntektEndring);
        } else if (andelEndring.getAktivitetStatus().erFrilanser()) {
            opprettHistorikkFrilansinntekt(tekstBuilder, inntektEndring);
        } else if (AktivitetStatus.DAGPENGER.equals(andelEndring.getAktivitetStatus())) {
            opprettHistorikkDagpengeinntekt(tekstBuilder, inntektEndring);
        } else {
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
                andelEndring.getAktivitetStatus().getNavn(),
                inntektEndring.getFraMånedsinntekt(),
                inntektEndring.getTilMånedsinntekt());
        }
    }

    private void opprettHistorikkArbeidstakerInntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, InntektEndring inntektEndring) {
        if (OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            opprettHistorikkEtterlønnSluttpakke(tekstBuilder, inntektEndring);
        } else if (andelEndring.getArbeidsgiver().isPresent()) {
            opprettHistorikkArbeidsinntekt(
                tekstBuilder,
                arbeidsforholdOverstyringer,
                andelEndring,
                inntektEndring);
        }
    }

    private void opprettHistorikkDagpengeinntekt(HistorikkInnslagTekstBuilder tekstBuilder, InntektEndring inntektEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.DAGPENGER_INNTEKT,
            inntektEndring.getFraMånedsinntekt(),
            inntektEndring.getTilMånedsinntekt());
    }

    private void opprettHistorikkEtterlønnSluttpakke(HistorikkInnslagTekstBuilder tekstBuilder, InntektEndring inntektEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE,
            inntektEndring.getFraMånedsinntekt(),
            inntektEndring.getTilMånedsinntekt());
    }

    private void opprettHistorikkFrilansinntekt(HistorikkInnslagTekstBuilder tekstBuilder, InntektEndring inntektEndring) {
        tekstBuilder.medEndretFelt(
            HistorikkEndretFeltType.FRILANS_INNTEKT,
            inntektEndring.getFraMånedsinntekt(),
            inntektEndring.getTilMånedsinntekt());
    }

    private void opprettHistorikkArbeidsinntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, InntektEndring inntektEndring) {
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(
            andelEndring.getArbeidsgiver().get(),
            andelEndring.getArbeidsforholdRef(),
            arbeidsforholdOverstyringer);
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
            arbeidsforholdInfo,
            inntektEndring.getFraMånedsinntekt(),
            inntektEndring.getTilMånedsinntekt());
    }

}
