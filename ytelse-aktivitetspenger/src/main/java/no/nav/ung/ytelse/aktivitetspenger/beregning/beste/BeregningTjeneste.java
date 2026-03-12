package no.nav.ung.ytelse.aktivitetspenger.beregning.beste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.grunnbeløp.Grunnbeløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;

import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.PgiKalkulator.avgrensOgOppjusterÅrsinntekter;
import static no.nav.ung.ytelse.aktivitetspenger.beregning.beste.PgiKalkulator.lagPgiKalkulatorInput;


@ApplicationScoped
public class BeregningTjeneste {

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

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
        var pgiKalkulatorInput = lagPgiKalkulatorInput(input);
        var pgiPerÅr = avgrensOgOppjusterÅrsinntekter(pgiKalkulatorInput);

        BigDecimal årsinntektSisteÅr = pgiPerÅr.getOrDefault(input.sisteLignedeÅr(), BigDecimal.ZERO);
        BigDecimal årsinntektSisteTreÅr = hentSnittTreSisteÅr(pgiPerÅr);
        BigDecimal beregningsgrunnlag = årsinntektSisteÅr.max(årsinntektSisteTreÅr);

        String regelSporing = LagRegelSporing.lagRegelSporingFraTidslinjer(lagRegelSporingMap(pgiKalkulatorInput));

        return new BesteberegningResultat(input, årsinntektSisteÅr, årsinntektSisteTreÅr, beregningsgrunnlag, regelSporing);
    }

    private static Map<String, LocalDateTimeline<?>> lagRegelSporingMap(PgiKalkulatorInput input) {
        var map = new LinkedHashMap<String, LocalDateTimeline<?>>();
        map.put("gsnittTidsserie", input.gsnittTidsserie().mapValue(Grunnbeløp::verdi));
        map.put("oppjusteringsfaktorTidsserie", input.oppjusteringsfaktorTidsserie());
        return map;
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
