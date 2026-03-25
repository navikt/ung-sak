package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerBeregningsgrunnlagRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

@ApplicationScoped
public class BeregningStegTjeneste {

    private static final BigDecimal DEKNINGSGRAD = BigDecimal.valueOf(0.66);

    private AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository;
    private BeregningTjeneste beregningTjeneste;

    BeregningStegTjeneste() {
    }

    @Inject
    public BeregningStegTjeneste(AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository,
                                 BeregningTjeneste beregningTjeneste) {
        this.aktivitetspengerBeregningsgrunnlagRepository = aktivitetspengerBeregningsgrunnlagRepository;
        this.beregningTjeneste = beregningTjeneste;
    }

    // TODO: Koble på utledning av siste tilgjengelige lignede år
    private Year utledSistLignedeÅr(LocalDate skjæringstidspunkt) {
        LocalDate maiFørste = LocalDate.of(LocalDate.now().getYear(), 5, 1);
        if (skjæringstidspunkt.isBefore(maiFørste)) {
            return Year.of(skjæringstidspunkt.minusYears(2).getYear());
        } else {
            return Year.of(skjæringstidspunkt.minusYears(1).getYear());
        }
    }

    public void utførBesteberegning(Long behandlingId, LocalDate skjæringstidspunkt) {
        var inntektsposter = beregningTjeneste.hentSigrunInntektsposter(behandlingId);
        var sistLignedeÅr = utledSistLignedeÅr(skjæringstidspunkt);

        var beregningInput = BeregningTjeneste.lagBeregningInput(sistLignedeÅr, skjæringstidspunkt, inntektsposter);
        var besteBeregningResultat = BeregningTjeneste.avgjørBesteberegning(beregningInput);

        BigDecimal beregningsgrunnlagRedusert = besteBeregningResultat.getBeregningsgrunnlag().multiply(DEKNINGSGRAD);

        var beregningsgrunnlag = new Beregningsgrunnlag(besteBeregningResultat.getBeregningInput(), besteBeregningResultat.getÅrsinntektSisteÅr(), besteBeregningResultat.getÅrsinntektSisteTreÅr(), besteBeregningResultat.getBeregningsgrunnlag(), beregningsgrunnlagRedusert, besteBeregningResultat.getRegelSporing());
        aktivitetspengerBeregningsgrunnlagRepository.lagreBeregningsgrunnlag(behandlingId, beregningsgrunnlag);
    }
}
