package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BeregningTjeneste.class);

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


    public Year utledSisteLignedeÅr(LocalDate skjæringstidspunkt, List<Inntektspost> inntektsposter) {
        LocalDate grenseForNårLigningsverdiKanVæreTilgjengelig = LocalDate.of(skjæringstidspunkt.getYear(), 5, 1);
        LocalDate grenseForNårLigningsverdiMåVæreTilgjengelig = LocalDate.of(skjæringstidspunkt.getYear(), 11, 30);

        if (skjæringstidspunkt.isBefore(grenseForNårLigningsverdiKanVæreTilgjengelig)) {
            return Year.of(skjæringstidspunkt.minusYears(2).getYear());
        } else {
            var forrigeÅr = Year.of(skjæringstidspunkt.minusYears(1).getYear());
            var senesteTilgjengeligeLigningsår = inntektsposter.stream()
                .map(ip -> Year.from(ip.getPeriode().getFomDato()))
                .max(Year::compareTo);

            if (senesteTilgjengeligeLigningsår.isPresent() && forrigeÅr.isAfter(senesteTilgjengeligeLigningsår.get())) {
                if (skjæringstidspunkt.isAfter(grenseForNårLigningsverdiMåVæreTilgjengelig)) {
                    LOGGER.warn("Forrige år er ikke tilgjengelig fra skatt, men grensen for når det senest skal kunne brukes er passert");
                }

                if (senesteTilgjengeligeLigningsår.get().isBefore(Year.of(skjæringstidspunkt.getYear() - 2))) {
                    throw new IllegalStateException("Siste tilgjengelige ligningsår fra skatt er over to år gammelt. Dette er ikke forventet og vil påvirke beregningen.");
                }

                return senesteTilgjengeligeLigningsår.get();
            }
            return forrigeÅr;
        }
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
        var oppjustertOgAvgrensetÅrsinntekt = pgiKalkulator.avgrensOgOppjusterÅrsinntekter();

        if (oppjustertOgAvgrensetÅrsinntekt.size() < 3) { throw new IllegalStateException("BesteBeregning: Kan ikke utføre besteberegning uten komplette data."); }

        BigDecimal årsinntektSisteÅr = oppjustertOgAvgrensetÅrsinntekt.getOrDefault(input.sisteLignedeÅr(), BigDecimal.ZERO);
        BigDecimal årsinntektSisteTreÅr = hentSnittTreSisteÅr(oppjustertOgAvgrensetÅrsinntekt);
        BigDecimal beregningsgrunnlag = årsinntektSisteÅr.max(årsinntektSisteTreÅr);

        String regelSporing = LagRegelSporing.lagRegelSporingFraTidslinjer(pgiKalkulator.getRegelSporingsmap());

        return new BesteberegningResultat(input, årsinntektSisteÅr, årsinntektSisteTreÅr, beregningsgrunnlag, pgiKalkulator.getGrunnbeløpVedSkjæringstidspunkt(), regelSporing);
    }

    private static BigDecimal hentSnittTreSisteÅr(Map<Year, BigDecimal> oppjustertOgAvgrensetÅrsinntekt) {
        return oppjustertOgAvgrensetÅrsinntekt.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .limit(3)
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);
    }
}
