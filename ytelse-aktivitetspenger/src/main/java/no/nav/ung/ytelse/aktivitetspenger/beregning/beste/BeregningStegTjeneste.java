package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerBeregningsgrunnlagRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class BeregningStegTjeneste {

    private static BigDecimal DEKNINGSGRAD = BigDecimal.valueOf(0.66);

    private AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    BeregningStegTjeneste() {
    }

    @Inject
    public BeregningStegTjeneste(AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.aktivitetspengerBeregningsgrunnlagRepository = aktivitetspengerBeregningsgrunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void utførBesteberegning(Long behandlingId, LocalDate skjæringstidspunkt) {
        var inntektsposter = hentSigrunInntektsposter(behandlingId);
        var sistLignedeÅr = Year.of(skjæringstidspunkt.minusYears(1).getYear());  // TODO: Koble på utledning av siste tilgjengelige lignede år

        var beregningInput = BeregningTjeneste.lagBeregningInput(sistLignedeÅr, skjæringstidspunkt, inntektsposter);
        var besteBeregningResultat = BeregningTjeneste.avgjørBesteberegning(beregningInput);

        BigDecimal beregningsgrunnlagRedusert = besteBeregningResultat.getBeregningsgrunnlag().multiply(DEKNINGSGRAD);

        var beregningsgrunnlag = new Beregningsgrunnlag(besteBeregningResultat.getBeregningInput(), besteBeregningResultat.getÅrsinntektSisteÅr(), besteBeregningResultat.getÅrsinntektSisteTreÅr(), besteBeregningResultat.getBeregningsgrunnlag(), beregningsgrunnlagRedusert, besteBeregningResultat.getRegelSporing());
        aktivitetspengerBeregningsgrunnlagRepository.lagreBeregningsgrunnlag(behandlingId, beregningsgrunnlag);
    }

    private List<Inntektspost> hentSigrunInntektsposter(Long behandlingId) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (iayGrunnlag.isEmpty()) {
            return Collections.emptyList();
        }

        var inntekter = iayGrunnlag.get().getRegisterVersjon().map(InntektArbeidYtelseAggregat::getInntekter);
        Collection<Inntektspost> inntektsposter = new InntektFilter(inntekter).getInntektsposter(InntektsKilde.SIGRUN);
        return List.copyOf(inntektsposter);
    }
}
