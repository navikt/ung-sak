package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetIdentifikator;

public class NyoppstartetUtleder {

    private static final Period NYOPPSTARTET_VARIGHET = Period.ofWeeks(4);
    private static final Period OPPHOLD_FØR_NYOPPSTARTET_IGJEN = Period.ofWeeks(2);

    public Map<Arbeidsgiver, LocalDateTimeline<Boolean>> utledPerioderMedNyoppstartetArbeidsforhold(InntektArbeidYtelseGrunnlag iayGrunnlag, AktørId bruker) {
        var aktørArbeid = iayGrunnlag.getAktørArbeidFraRegister(bruker);
        if (aktørArbeid.isEmpty()) {
            return Map.of();
        }

        Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> aktiveArbeidsforhold = utledAktivTidslinjePrArbeidsforhold(aktørArbeid.get());
        Map<Arbeidsgiver, LocalDateTimeline<Boolean>> aktiveArbeidssteder = utledAktivTidslinjePrArbeidsgiver(aktiveArbeidsforhold);
        return utledNyoppstartetPerioder(aktiveArbeidssteder);
    }

    private Map<Arbeidsgiver, LocalDateTimeline<Boolean>> utledNyoppstartetPerioder(Map<Arbeidsgiver, LocalDateTimeline<Boolean>> aktiveArbeidssteder) {
        return aktiveArbeidssteder.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> nyoppstartetPerioder(entry.getValue())));
    }

    static LocalDateTimeline<Boolean> nyoppstartetPerioder(LocalDateTimeline<Boolean> aktivTidslinje) {
        List<LocalDate> startdatoer = aktivTidslinje.compress().stream().map(LocalDateSegment::getFom).toList();
        return new LocalDateTimeline<>(startdatoer.stream()
            .filter(startdato -> {
                LocalDateTimeline<Boolean> periodeSomMåVæreUtenArbeidForAtDetteErNyStart = new LocalDateTimeline<>(startdato.minus(OPPHOLD_FØR_NYOPPSTARTET_IGJEN), startdato.minusDays(1), true);
                return !aktivTidslinje.intersects(periodeSomMåVæreUtenArbeidForAtDetteErNyStart);
            })
            .map(startdato -> new LocalDateSegment<>(startdato, startdato.plus(NYOPPSTARTET_VARIGHET).minusDays(1), true))
            .toList(), StandardCombinators::alwaysTrueForMatch)
            .intersection(aktivTidslinje);
    }

    private Map<Arbeidsgiver, LocalDateTimeline<Boolean>> utledAktivTidslinjePrArbeidsgiver(Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> aktiveArbeidsforhold) {
        Map<Arbeidsgiver, LocalDateTimeline<Boolean>> aktivHosArbeidsgiver = new HashMap<>();
        for (var entry : aktiveArbeidsforhold.entrySet()) {
            Arbeidsgiver arbeidsgiver = entry.getKey().getArbeidsgiverArbeidsforhold().getArbeidsgiver();
            LocalDateTimeline<Boolean> aktivTidslinje = aktivHosArbeidsgiver.getOrDefault(arbeidsgiver, LocalDateTimeline.empty());
            aktivTidslinje = aktivTidslinje.crossJoin(entry.getValue(), StandardCombinators::alwaysTrueForMatch);
            aktivHosArbeidsgiver.put(arbeidsgiver, aktivTidslinje);
        }
        return aktivHosArbeidsgiver;
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> utledAktivTidslinjePrArbeidsforhold(AktørArbeid aktørArbeid) {
        Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> aktiveArbeidsforhold = new HashMap<>();
        for (Yrkesaktivitet yrkesaktivitet : aktørArbeid.hentAlleYrkesaktiviteter()) {
            var aktivitetIdentifikator = AktivitetIdentifikator.lagAktivitetIdentifikator(UttakArbeidType.ARBEIDSTAKER, yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef());
            var aktivitetTidslinje = mapYrkesAktivitet(yrkesaktivitet);
            aktiveArbeidsforhold.put(aktivitetIdentifikator, aktivitetTidslinje);
        }
        return aktiveArbeidsforhold;
    }

    private LocalDateTimeline<Boolean> mapYrkesAktivitet(Yrkesaktivitet yrkesaktivitet) {
        var segmenter = yrkesaktivitet.getAnsettelsesPeriode()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .toList();
        // Har ikke helt kontroll på aa-reg mtp overlapp her så better safe than sorry (bruker combinator)
        var arbeidsAktivTidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceRightHandSide);
        // Ta bort permisjoner
        var permisjonTidslinje = mapPermisjon(yrkesaktivitet);
        arbeidsAktivTidslinje = arbeidsAktivTidslinje.disjoint(permisjonTidslinje);
        return arbeidsAktivTidslinje.compress();
    }

    private LocalDateTimeline<Boolean> mapPermisjon(Yrkesaktivitet yrkesaktivitet) {
        var relevantePermitteringer = yrkesaktivitet.getPermisjon().stream()
            .filter(it -> erStørreEllerLik100Prosent(it.getProsentsats()))
            .map(it -> new LocalDateSegment<>(it.getFraOgMed(), it.getTilOgMed(), true))
            .toList();

        return new LocalDateTimeline<>(relevantePermitteringer, StandardCombinators::coalesceRightHandSide)
            .compress();
    }

    private boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }

}
