package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class EtterlønnSluttpakkeTjeneste {

    private EtterlønnSluttpakkeTjeneste() {
        // Skjul
    }

    public static boolean skalVurdereOmBrukerHarEtterlønnSluttpakke(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().orElse(null);
        Objects.requireNonNull(beregningsgrunnlag, "beregningsgrunnlag");
        return søkerErArbeidstaker(beregningsgrunnlag) && søkerHarBGAndelForEtterlønnSluttpakke(beregningsgrunnlag);
    }

    private static boolean søkerHarBGAndelForEtterlønnSluttpakke(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        List<BeregningsgrunnlagPrStatusOgAndel> alleAndeler = beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        return alleAndeler.stream().anyMatch(andel -> OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE.equals(andel.getArbeidsforholdType()));
    }

    private static boolean søkerErArbeidstaker(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getAktivitetStatuser().stream().anyMatch(as -> as.getAktivitetStatus().erArbeidstaker());
    }
}
