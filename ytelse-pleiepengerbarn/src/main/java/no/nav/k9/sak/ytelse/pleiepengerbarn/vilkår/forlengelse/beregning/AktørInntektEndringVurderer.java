package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.sak.domene.iay.modell.AktørInntekt;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

class AktørInntektEndringVurderer {

    static boolean harEndringIInntekt(Optional<AktørInntekt> aktørArbeid, Optional<AktørInntekt> originalAktørArbeid, DatoIntervallEntitet periode) {
        if (aktørArbeid.isEmpty() || originalAktørArbeid.isEmpty()) {
            return aktørArbeid.isEmpty() == originalAktørArbeid.isEmpty();
        }
        var inntektAggregat = aktørArbeid.get();
        var originalInntektAggregat = originalAktørArbeid.get();

        var skjæringstidspunkt = periode.getFomDato();

        if (harEndringISammenligningsfilter(inntektAggregat, originalInntektAggregat, skjæringstidspunkt)) {
            return true;
        }

        if (harEndringIBeregningfilter(inntektAggregat, originalInntektAggregat, skjæringstidspunkt)) {
            return true;
        }

        return false;
    }


    private static boolean harEndringIBeregningfilter(AktørInntekt inntektAggregat, AktørInntekt originalInntektAggregat, LocalDate skjæringstidspunkt) {
        var relevanteInntekterForSammenligning = finnRelevanteInntekterForBeregningPrArbeidsgiver(inntektAggregat, skjæringstidspunkt);
        var orignalRelevanteInntekterForSammenligning = finnRelevanteInntekterForBeregningPrArbeidsgiver(originalInntektAggregat, skjæringstidspunkt);
        if (harEndringIInntekter(relevanteInntekterForSammenligning, orignalRelevanteInntekterForSammenligning)) {
            return true;
        }
        return false;
    }

    private static boolean harEndringISammenligningsfilter(AktørInntekt inntektAggregat, AktørInntekt originalInntektAggregat, LocalDate skjæringstidspunkt) {
        var relevanteInntekterForSammenligning = finnRelevanteInntekterForSammeligningPrArbeidsgiver(inntektAggregat, skjæringstidspunkt);
        var orignalRelevanteInntekterForSammenligning = finnRelevanteInntekterForSammeligningPrArbeidsgiver(originalInntektAggregat, skjæringstidspunkt);
        if (harEndringIInntekter(relevanteInntekterForSammenligning, orignalRelevanteInntekterForSammenligning)) {
            return true;
        }
        return false;
    }

    private static boolean harEndringIInntekter(Map<Arbeidsgiver, List<Inntektspost>> relevanteInntekterForSammenligning, Map<Arbeidsgiver, List<Inntektspost>> orignalRelevanteInntekterForSammenligning) {
        for (var inntektPrArbeidsgiverEntry : relevanteInntekterForSammenligning.entrySet()) {
            var originalInntektsposter = orignalRelevanteInntekterForSammenligning.get(inntektPrArbeidsgiverEntry.getKey());
            if (originalInntektsposter == null) {
                return true;
            }
            if (harEndringIInntektsposter(inntektPrArbeidsgiverEntry.getValue(), originalInntektsposter)) {
                return true;
            }
        }
        return false;
    }

    private static boolean harEndringIInntektsposter(List<Inntektspost> inntektsposter, List<Inntektspost> originalInntektsposter) {
        if (inntektsposter.size() != originalInntektsposter.size()) {
            return true;
        }
        for (var inntektspost : inntektsposter) {
            var originalPost = originalInntektsposter.stream().filter(p ->
                p.getPeriode().equals(inntektspost.getPeriode()) &&
                p.getInntektspostType().equals(inntektspost.getInntektspostType()) &&
                p.getBeløp().equals(inntektspost.getBeløp())).findFirst();
            if (originalPost.isEmpty()) {
                return true;
            }
        }
        return false;
    }


    private static Map<Arbeidsgiver, List<Inntektspost>> finnRelevanteInntekterForSammeligningPrArbeidsgiver(AktørInntekt inntektAggregat, LocalDate skjæringstidspunkt) {
        return inntektAggregat.getInntekt().stream()
            .filter(i -> i.getInntektsKilde().equals(InntektsKilde.INNTEKT_SAMMENLIGNING))
            .collect(Collectors.groupingBy(Inntekt::getArbeidsgiver,
                Collectors.flatMapping(i -> i.getAlleInntektsposter().stream()
                        .filter(ip -> !ip.getPeriode().getFomDato().isBefore(skjæringstidspunkt.minusMonths(11).withDayOfMonth(1))),
                    Collectors.toList())));
    }

    private static Map<Arbeidsgiver, List<Inntektspost>> finnRelevanteInntekterForBeregningPrArbeidsgiver(AktørInntekt inntektAggregat, LocalDate skjæringstidspunkt) {
        return inntektAggregat.getInntekt().stream()
            .filter(i -> i.getInntektsKilde().equals(InntektsKilde.INNTEKT_BEREGNING))
            .collect(Collectors.groupingBy(Inntekt::getArbeidsgiver,
                Collectors.flatMapping(i -> i.getAlleInntektsposter().stream()
                        .filter(ip -> !ip.getPeriode().getFomDato().isBefore(skjæringstidspunkt.minusMonths(4).withDayOfMonth(1))),
                    Collectors.toList())));
    }


}
