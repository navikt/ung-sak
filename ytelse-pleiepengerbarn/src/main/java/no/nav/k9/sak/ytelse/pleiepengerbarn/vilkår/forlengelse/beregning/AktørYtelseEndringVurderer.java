package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class AktørYtelseEndringVurderer {

    static boolean harEndringIYtelse(Optional<AktørYtelse> aktørYtelse, Optional<AktørYtelse> originalAktørYtelse, DatoIntervallEntitet periode) {
        if (aktørYtelse.isEmpty() || originalAktørYtelse.isEmpty()) {
            return aktørYtelse.isEmpty() == originalAktørYtelse.isEmpty();
        }
        var ytelseAggregat = aktørYtelse.get();
        var originalYtelseAggregat = originalAktørYtelse.get();
        var skjæringstidspunkt = periode.getFomDato();

        if (harEndringIMeldekortYtelser(ytelseAggregat, originalYtelseAggregat, skjæringstidspunkt)) {
            return true;
        }

        if (harEndringIAndreYtelser(ytelseAggregat, originalYtelseAggregat, skjæringstidspunkt)) {
            return true;
        }

        return false;
    }

    private static boolean harEndringIAndreYtelser(AktørYtelse ytelseAggregat, AktørYtelse originalYtelseAggregat, LocalDate skjæringstidspunkt) {
        var relevanteYtelser = finnRelevanteYtelser(ytelseAggregat, skjæringstidspunkt);
        var originalRelevanteYtelser = finnRelevanteYtelser(originalYtelseAggregat, skjæringstidspunkt);

        for (Ytelse ytelse : relevanteYtelser) {
            var originalMatch = originalRelevanteYtelser.stream()
                .filter(y -> y.getYtelseType().equals(ytelse.getYtelseType()))
                .filter(y -> y.getPeriode().equals(ytelse.getPeriode()))
                .findFirst();
            if (originalMatch.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean harEndringIMeldekortYtelser(AktørYtelse ytelseAggregat, AktørYtelse originalYtelseAggregat, LocalDate skjæringstidspunkt) {
        var relevantMeldekortYtelser = finnRelevantDagpengerEllerArbeidsavklaringspenger(ytelseAggregat, skjæringstidspunkt);
        var originalMeldekortytelser = finnRelevantDagpengerEllerArbeidsavklaringspenger(originalYtelseAggregat, skjæringstidspunkt);

        if (relevantMeldekortYtelser.size() != originalMeldekortytelser.size()) {
            return true;
        }

        for (Ytelse ytelse : relevantMeldekortYtelser) {
            var originalMatch = originalMeldekortytelser.stream()
                .filter(y -> y.getYtelseType().equals(ytelse.getYtelseType()))
                .findFirst();
            if (originalMatch.isEmpty() || harEndringIMeldekort(ytelse, originalMatch.get(), skjæringstidspunkt)) {
                return true;
            }
        }
        return false;
    }

    private static boolean harEndringIMeldekort(Ytelse ytelse, Ytelse originalYtelse, LocalDate skjæringstidspunkt) {
        var relevanteMeldekort = ytelse.getYtelseAnvist().stream().filter(erRelevant(skjæringstidspunkt))
            .toList();
        var originaleMeldekort = originalYtelse.getYtelseAnvist().stream().filter(erRelevant(skjæringstidspunkt))
            .toList();
        for (YtelseAnvist meldekort : relevanteMeldekort) {
            var matchendeMeldekort = originaleMeldekort.stream()
                .filter(m -> m.getAnvistFOM().equals(meldekort.getAnvistFOM()) && m.getAnvistTOM().equals(meldekort.getAnvistFOM()) && m.getDagsats().orElseThrow().equals(meldekort.getDagsats().orElseThrow()))
                .findFirst();
            if (matchendeMeldekort.isEmpty()) {
                return true;
            }
        }
        return false;
    }


    private static List<Ytelse> finnRelevantDagpengerEllerArbeidsavklaringspenger(AktørYtelse ytelseAgregat, LocalDate skjæringstidspunkt) {
        return ytelseAgregat.getAlleYtelser().stream()
            .filter(y -> y.getYtelseType().equals(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER) || y.getYtelseType().equals(FagsakYtelseType.DAGPENGER))
            .filter(ya -> ya.getYtelseAnvist().stream().anyMatch(erRelevant(skjæringstidspunkt)))
            .toList();
    }

    private static List<Ytelse> finnRelevanteYtelser(AktørYtelse ytelseAgregat, LocalDate skjæringstidspunkt) {
        return ytelseAgregat.getAlleYtelser().stream()
            .filter(y -> !y.getYtelseType().equals(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER) && !y.getYtelseType().equals(FagsakYtelseType.DAGPENGER))
            .filter(y -> y.getPeriode().inkluderer(skjæringstidspunkt.minusDays(1)))
            .toList();
    }


    @NotNull
    private static Predicate<YtelseAnvist> erRelevant(LocalDate skjæringstidspunkt) {
        return ap -> ap.getAnvistTOM().isAfter(skjæringstidspunkt.minusMonths(4).withDayOfYear(1));
    }


}
