package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@ApplicationScoped
public class BeregningStegTjeneste {

    private static final BigDecimal DEKNINGSGRAD = BigDecimal.valueOf(0.66);

    private BeregningTjeneste beregningTjeneste;

    BeregningStegTjeneste() {
    }

    @Inject
    public BeregningStegTjeneste(BeregningTjeneste beregningTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
    }

    public Beregningsgrunnlag utførBesteberegning(Long behandlingId, LocalDate skjæringstidspunkt) {
        var inntektsposter = beregningTjeneste.hentSigrunInntektsposter(behandlingId);

        var sistLignedeÅr = beregningTjeneste.utledSisteLignedeÅr(skjæringstidspunkt, inntektsposter);
        var beregningInput = BeregningTjeneste.lagBeregningInput(sistLignedeÅr, skjæringstidspunkt, inntektsposter);
        var besteBeregningResultat = BeregningTjeneste.avgjørBesteberegning(beregningInput);

        BigDecimal beregningsgrunnlagRedusert = besteBeregningResultat.getBeregningsgrunnlag().multiply(DEKNINGSGRAD);

        return new Beregningsgrunnlag(besteBeregningResultat.getBeregningInput(), besteBeregningResultat.getÅrsinntektSisteÅr(), besteBeregningResultat.getÅrsinntektSisteTreÅr(), besteBeregningResultat.getBeregningsgrunnlag(), beregningsgrunnlagRedusert, besteBeregningResultat.getGrunnbeløp(), besteBeregningResultat.getRegelSporing());
    }
}
