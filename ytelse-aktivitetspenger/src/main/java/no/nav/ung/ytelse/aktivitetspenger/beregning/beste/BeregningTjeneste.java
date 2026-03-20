package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BeregningTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public BeregningTjeneste() {
    }

    @Inject
    public BeregningTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public List<Inntektspost> hentSigrunInntektsposter(Long behandlingId) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (iayGrunnlag.isEmpty()) {
            return Collections.emptyList();
        }

        var inntekter = iayGrunnlag.get().getRegisterVersjon().map(InntektArbeidYtelseAggregat::getInntekter);
        Collection<Inntektspost> inntektsposter = new InntektFilter(inntekter).getInntektsposter(InntektsKilde.SIGRUN);
        return List.copyOf(inntektsposter);
    }

    public static BeregningInput lagBeregningInput(Year sisteTilgjengeligeLigningsår, LocalDate skjæringstidspunkt, List<Inntektspost> inntekter) {
        PgiHjelper pgiHjelper = new PgiHjelper(inntekter, sisteTilgjengeligeLigningsår);
        return new BeregningInput(
            pgiHjelper.getPgi1(),
            pgiHjelper.getPgi2(),
            pgiHjelper.getPgi3(),
            skjæringstidspunkt,
            sisteTilgjengeligeLigningsår
        );
    }

    public static BesteberegningResultat avgjørBesteberegning(BeregningInput input) {
        var pgiKalkulator = new PgiKalkulator(input);
        var pgiPerÅr = pgiKalkulator.avgrensOgOppjusterÅrsinntekter();

        BigDecimal årsinntektSisteÅr = pgiPerÅr.getOrDefault(input.sisteLignedeÅr(), BigDecimal.ZERO);
        BigDecimal årsinntektSisteTreÅr = hentSnittTreSisteÅr(pgiPerÅr);
        BigDecimal beregningsgrunnlag = årsinntektSisteÅr.max(årsinntektSisteTreÅr);

        String regelSporing = LagRegelSporing.lagRegelSporingFraTidslinjer(pgiKalkulator.getRegelSporingsmap());

        return new BesteberegningResultat(input, årsinntektSisteÅr, årsinntektSisteTreÅr, beregningsgrunnlag, regelSporing);
    }

    private static BigDecimal hentSnittTreSisteÅr(Map<Year, BigDecimal> pgiPerÅr) {
        return pgiPerÅr.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .limit(3)
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(3), 10, java.math.RoundingMode.HALF_EVEN);
    }
}
