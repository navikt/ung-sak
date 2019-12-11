package no.nav.folketrygdloven.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType.ARBEIDSAVKLARINGSPENGER;
import static no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType.DAGPENGER;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;

public class AutopunktUtlederFastsettBeregningsaktiviteterTjeneste {

    private AutopunktUtlederFastsettBeregningsaktiviteterTjeneste() {
        // Skjuler default konstruktør
    }

    /**
     * Utleder om det skal ventes på siste meldekort for AAP for å kunne beregne riktig beregningsgrunnlag.
     * Skal vente om:
     * - Har annen aktivitetstatus enn AAP
     * - Har løpende AAP på skjæringstidspunktet
     * - Har sendt inn meldekort for AAP de siste 4 mnd før skjæringstidspunkt for opptjening
     *
     * @param aktørYtelse aktørytelse for søker
     * @param beregningsgrunnlag beregningsgrunnlaget
     * @param dagensdato Dagens dato/ idag
     * @return Optional som innholder ventefrist om autopunkt skal opprettes, Optional.empty ellers
     */
    static Optional<LocalDate> skalVenteTilDatoPåMeldekortAAPellerDP(Optional<AktørYtelse> aktørYtelse, BeregningsgrunnlagEntitet beregningsgrunnlag, LocalDate dagensdato) {
        if (!harLøpendeVedtakOgSendtInnMeldekortNylig(aktørYtelse, beregningsgrunnlag.getSkjæringstidspunkt()))
            return Optional.empty();

        return utledVenteFrist(beregningsgrunnlag.getSkjæringstidspunkt(), dagensdato);
    }

    private static boolean harLøpendeVedtakOgSendtInnMeldekortNylig(Optional<AktørYtelse> aktørYtelse, LocalDate skjæringstidspunkt) {
        List<Ytelse> aapOgDPYtelser = getAAPogDPYtelser(aktørYtelse, skjæringstidspunkt);
        boolean hattAAPSiste4Mnd = hattGittYtelseIGittPeriode(aapOgDPYtelser, skjæringstidspunkt.minusMonths(4).withDayOfMonth(1),
            ARBEIDSAVKLARINGSPENGER);
        Predicate<List<Ytelse>> hattDPSiste10Mnd = ytelser -> hattGittYtelseIGittPeriode(ytelser, skjæringstidspunkt.minusMonths(10), DAGPENGER);

        if (!hattAAPSiste4Mnd && !hattDPSiste10Mnd.test(aapOgDPYtelser)) {
            return false;
        }

        RelatertYtelseType ytelseType = hattAAPSiste4Mnd ? ARBEIDSAVKLARINGSPENGER : DAGPENGER;
        return aapOgDPYtelser.stream()
            .filter(ytelse -> ytelseType.equals(ytelse.getRelatertYtelseType()))
            .anyMatch(ytelse -> ytelse.getPeriode().getFomDato().isBefore(skjæringstidspunkt)
                && !ytelse.getPeriode().getTomDato().isBefore(skjæringstidspunkt.minusDays(1)));
    }

    private static boolean hattGittYtelseIGittPeriode(List<Ytelse> aapOgDPYtelser, LocalDate hattYtelseFom, RelatertYtelseType ytelseType) {
        return aapOgDPYtelser.stream()
            .filter(ytelse -> ytelseType.equals(ytelse.getRelatertYtelseType()))
            .flatMap(ytelse -> ytelse.getYtelseAnvist().stream())
            .anyMatch(ya -> !ya.getAnvistTOM().isBefore(hattYtelseFom));
    }

    private static List<Ytelse> getAAPogDPYtelser(Optional<AktørYtelse> aktørYtelse, LocalDate skjæringstidspunkt) {
        var filter = new YtelseFilter(aktørYtelse).før(skjæringstidspunkt);
        var ytelser = filter.getFiltrertYtelser().stream()
            .filter(ytelse -> ARBEIDSAVKLARINGSPENGER.equals(ytelse.getRelatertYtelseType()) || DAGPENGER.equals(ytelse.getRelatertYtelseType()))
            .collect(Collectors.toList());
        return ytelser;
    }

    private static Optional<LocalDate> utledVenteFrist(LocalDate skjæringstidspunktOpptjening, LocalDate dagensdato) {
        if (!dagensdato.isAfter(skjæringstidspunktOpptjening)) {
            return Optional.of(skjæringstidspunktOpptjening.plusDays(1));
        }
        if (!dagensdato.isAfter(skjæringstidspunktOpptjening.plusDays(14))) {
            return Optional.of(dagensdato.plusDays(1));
        }
        return Optional.empty();
    }
}
