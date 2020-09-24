package no.nav.k9.sak.web.app.tjenester.behandling.historikk;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.RefusjonEndring;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

/**
 * Historikktjeneste for endring av inntekt
 */
@Dependent
class RefusjonHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    public RefusjonHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public RefusjonHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    public void lagHistorikkOmEndret(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring) {
        Optional<RefusjonEndring> refusjonEndring = andelEndring.getRefusjonEndring();
        refusjonEndring.ifPresent(endring -> opprettRefusjonHistorikk(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, endring));
    }

    private void opprettRefusjonHistorikk(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        if (andelEndring.getAktivitetStatus().erArbeidstaker()) {
            opprettHistorikkArbeidstakerInntekt(tekstBuilder, arbeidsforholdOverstyringer, andelEndring, refusjonEndring);
        }
    }

    private void opprettHistorikkArbeidstakerInntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        if (OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andelEndring.getArbeidsforholdType())) {
            opprettHistorikkEtterlønnSluttpakke(tekstBuilder, refusjonEndring);
        } else if (andelEndring.getArbeidsgiver().isPresent()) {
            opprettHistorikkArbeidsinntekt(
                tekstBuilder,
                arbeidsforholdOverstyringer,
                andelEndring,
                refusjonEndring);
        }
    }

    private void opprettHistorikkEtterlønnSluttpakke(HistorikkInnslagTekstBuilder tekstBuilder, RefusjonEndring inntektEndring) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_ETTERLØNN_SLUTTPAKKE,
            inntektEndring.getFraRefusjon(),
            inntektEndring.getTilRefusjon());
    }

    private void opprettHistorikkArbeidsinntekt(HistorikkInnslagTekstBuilder tekstBuilder, List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer, BeregningsgrunnlagPrStatusOgAndelEndring andelEndring, RefusjonEndring refusjonEndring) {
        String arbeidsforholdInfo = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(
            andelEndring.getArbeidsgiver().get(),
            andelEndring.getArbeidsforholdRef(),
            arbeidsforholdOverstyringer);
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD,
            arbeidsforholdInfo,
            refusjonEndring.getFraRefusjon(),
            refusjonEndring.getTilRefusjon());
    }

}
