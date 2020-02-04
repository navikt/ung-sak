package no.nav.folketrygdloven.beregningsgrunnlag.felles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

public class BeregningUtils {

    private static final Period MELDEKORT_PERIODE_UTV = Period.parse("P30D");

    public static final BigDecimal MAX_UTBETALING_PROSENT_AAP_DAG = BigDecimal.valueOf(200);

    private BeregningUtils() {}

    public static Optional<Ytelse> sisteVedtakFørStpForType(YtelseFilter ytelseFilter, LocalDate skjæringstidspunkt, Set<FagsakYtelseType> ytelseTyper) {
        return ytelseFilter.getFiltrertYtelser().stream()
            .filter(ytelse -> ytelseTyper.contains(ytelse.getYtelseType()))
            .filter(ytelse -> !skjæringstidspunkt.isBefore(ytelse.getPeriode().getFomDato()))
            .max(Comparator.comparing(Ytelse::getPeriode).thenComparing(ytelse -> ytelse.getPeriode().getTomDato()));
    }

    public static Optional<YtelseAnvist> sisteHeleMeldekortFørStp(YtelseFilter ytelseFilter, Ytelse sisteVedtak, LocalDate skjæringstidspunkt, Set<FagsakYtelseType> ytelseTyper) {
        final LocalDate sisteVedtakFom = sisteVedtak.getPeriode().getFomDato();

        List<YtelseAnvist> alleMeldekort = ytelseFilter.getFiltrertYtelser().stream()
            .filter(ytelse -> ytelseTyper.contains(ytelse.getYtelseType()))
            .flatMap(ytelse -> ytelse.getYtelseAnvist().stream()).collect(Collectors.toList());

        Optional<YtelseAnvist> sisteMeldekort = alleMeldekort.stream()
            .filter(ytelseAnvist -> sisteVedtakFom.minus(MELDEKORT_PERIODE_UTV).isBefore(ytelseAnvist.getAnvistTOM()))
            .filter(ytelseAnvist -> skjæringstidspunkt.isAfter(ytelseAnvist.getAnvistTOM()))
            .max(Comparator.comparing(YtelseAnvist::getAnvistFOM));

        if (sisteMeldekort.isEmpty()) {
            return Optional.empty();
        }

        // Vi er nødt til å sjekke om vi har flere meldekort med samme periode
        List<YtelseAnvist> alleMeldekortMedPeriode = alleMeldekortMedPeriode(sisteMeldekort.get().getAnvistFOM(), sisteMeldekort.get().getAnvistTOM(), alleMeldekort);

        if (alleMeldekortMedPeriode.size() > 1) {
            return finnMeldekortSomGjelderForVedtak(alleMeldekortMedPeriode, sisteVedtak);
        }

        return sisteMeldekort;

    }

    private static List<YtelseAnvist> alleMeldekortMedPeriode(LocalDate anvistFOM, LocalDate anvistTOM, List<YtelseAnvist> alleMeldekort) {
        return alleMeldekort.stream()
            .filter(meldekort -> Objects.equals(meldekort.getAnvistFOM(), anvistFOM))
            .filter(meldekort -> Objects.equals(meldekort.getAnvistTOM(), anvistTOM))
            .collect(Collectors.toList());
    }

    private static Optional<YtelseAnvist> finnMeldekortSomGjelderForVedtak(List<YtelseAnvist> meldekort, Ytelse sisteVedtak) {
        return meldekort.stream().filter(m -> matcherMeldekortFraSisteVedtak(m, sisteVedtak)).findFirst();
    }

    private static boolean matcherMeldekortFraSisteVedtak(YtelseAnvist meldekort, Ytelse sisteVedtak) {
        return sisteVedtak.getYtelseAnvist().stream().anyMatch(ya -> Objects.equals(ya, meldekort));
    }
}
