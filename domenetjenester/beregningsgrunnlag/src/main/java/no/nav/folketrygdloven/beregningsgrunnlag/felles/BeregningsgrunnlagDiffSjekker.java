package no.nav.folketrygdloven.beregningsgrunnlag.felles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.vedtak.util.Tuple;

public class BeregningsgrunnlagDiffSjekker {

    private BeregningsgrunnlagDiffSjekker() {
        // Skjul
    }

    public static boolean harSignifikantDiffIBeregningsgrunnlag(BeregningsgrunnlagEntitet aktivt, BeregningsgrunnlagEntitet forrige) {
        if (!hentStatuser(aktivt).equals(hentStatuser(forrige))) {
            return true;
        }
        if (!aktivt.getSkjæringstidspunkt().equals(forrige.getSkjæringstidspunkt())) {
            return true;
        }
        List<BeregningsgrunnlagPeriode> aktivePerioder = aktivt.getBeregningsgrunnlagPerioder();
        List<BeregningsgrunnlagPeriode> forrigePerioder = forrige.getBeregningsgrunnlagPerioder();
        return harPeriodeDiff(aktivePerioder, forrigePerioder);
    }

    private static boolean harPeriodeDiff(List<BeregningsgrunnlagPeriode> aktivePerioder, List<BeregningsgrunnlagPeriode> forrigePerioder) {
        if (aktivePerioder.size() != forrigePerioder.size()) {
            return true;
        }
        // begge listene er sorter på fom dato så det er mulig å benytte indeks her
        for (int i = 0; i < aktivePerioder.size(); i++) {
            BeregningsgrunnlagPeriode aktivPeriode = aktivePerioder.get(i);
            BeregningsgrunnlagPeriode forrigePeriode = forrigePerioder.get(i);
            if (!aktivPeriode.getBeregningsgrunnlagPeriodeFom().equals(forrigePeriode.getBeregningsgrunnlagPeriodeFom())) {
                return true;
            }
            if (!erLike(aktivPeriode.getBruttoPrÅr(), forrigePeriode.getBruttoPrÅr())) {
                return true;
            }
            Tuple<List<BeregningsgrunnlagPrStatusOgAndel>, List<BeregningsgrunnlagPrStatusOgAndel>> resultat = finnAndeler(aktivPeriode, forrigePeriode);
            if (resultat.getElement1().size() != resultat.getElement2().size()) {
                return true;
            }
            if (sjekkAndeler(resultat.getElement1(), resultat.getElement2())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sjekkAndeler(List<BeregningsgrunnlagPrStatusOgAndel> aktiveAndeler, List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler) {
        for (BeregningsgrunnlagPrStatusOgAndel aktivAndel : aktiveAndeler) {
            Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndelOpt = forrigeAndeler
                .stream().filter(a -> a.getAndelsnr().equals(aktivAndel.getAndelsnr()))
                .findFirst();
            if (forrigeAndelOpt.isEmpty()) {
                return true;
            }
            BeregningsgrunnlagPrStatusOgAndel forrigeAndel = forrigeAndelOpt.get();
            if (harAndelDiff(aktivAndel, forrigeAndel)) {
                return true;
            }
        }
        return false;
    }

    private static boolean harAndelDiff(BeregningsgrunnlagPrStatusOgAndel aktivAndel, BeregningsgrunnlagPrStatusOgAndel forrigeAndel) {
        if (!aktivAndel.getAktivitetStatus().equals(forrigeAndel.getAktivitetStatus())) {
            return true;
        }
        if (hvisArbforManglerHosKunEn(aktivAndel, forrigeAndel)) {
            return true;
        }

        Optional<BGAndelArbeidsforhold> aktivArbeidsforhold = aktivAndel.getBgAndelArbeidsforhold();
        Optional<BGAndelArbeidsforhold> forrigeArbeidsforhold = forrigeAndel.getBgAndelArbeidsforhold();

        if (aktivArbeidsforhold.isPresent() && forrigeArbeidsforhold.isPresent()) {
            return aktivArbeidsforholdFørerTilDiff(aktivArbeidsforhold.get(), forrigeArbeidsforhold.get());
        }
        if (!aktivAndel.getInntektskategori().equals(forrigeAndel.getInntektskategori())) {
            return true;
        }
        if (!erLike(aktivAndel.getBruttoPrÅr(), forrigeAndel.getBruttoPrÅr())) {
            return true;
        }
        return false;
    }

    private static boolean hvisArbforManglerHosKunEn(BeregningsgrunnlagPrStatusOgAndel aktivAndel, BeregningsgrunnlagPrStatusOgAndel forrigeAndel) {
        return aktivAndel.getBgAndelArbeidsforhold().isPresent() != forrigeAndel.getBgAndelArbeidsforhold().isPresent();
    }

    private static boolean aktivArbeidsforholdFørerTilDiff(BGAndelArbeidsforhold aktivArbeidsforhold, BGAndelArbeidsforhold forrigeArbeidsforhold) {
        if (!aktivArbeidsforhold.getArbeidsgiver().equals(forrigeArbeidsforhold.getArbeidsgiver())) {
            return true;
        }
        if (!erLike(aktivArbeidsforhold.getRefusjonskravPrÅr(), forrigeArbeidsforhold.getRefusjonskravPrÅr())) {
            return true;
        }
        return false;
    }

    private static List<AktivitetStatus> hentStatuser(BeregningsgrunnlagEntitet aktivt) {
        return aktivt.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).collect(Collectors.toList());
    }

    private static Tuple<List<BeregningsgrunnlagPrStatusOgAndel>, List<BeregningsgrunnlagPrStatusOgAndel>> finnAndeler(BeregningsgrunnlagPeriode aktivPeriode, BeregningsgrunnlagPeriode forrigePeriode) {
        List<BeregningsgrunnlagPrStatusOgAndel> aktiveAndeler = aktivPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler = forrigePeriode
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> !a.getLagtTilAvSaksbehandler())
            .collect(Collectors.toList());
        return new Tuple<>(aktiveAndeler, forrigeAndeler);
    }

    private static boolean erLike(BigDecimal verdi1, BigDecimal verdi2) {
        return verdi1 == null && verdi2 == null || verdi1 != null && verdi1.compareTo(verdi2) == 0;
    }
}
