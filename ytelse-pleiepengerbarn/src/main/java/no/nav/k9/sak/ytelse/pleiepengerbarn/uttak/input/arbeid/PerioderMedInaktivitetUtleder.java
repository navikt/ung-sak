package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;

public class PerioderMedInaktivitetUtleder {

    public Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> utled(InaktivitetUtlederInput input) {
        var tidslinjeTilVurdering = input.getTidslinjeTilVurdering();

        if (tidslinjeTilVurdering.isEmpty()) {
            return Map.of();
        }

        var ikkeAktivTidslinje = new LocalDateTimeline<>(tidslinjeTilVurdering.toSegments()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .collect(Collectors.toList()));

        var aktørArbeid = input.getIayGrunnlag().getAktørArbeidFraRegister(input.getBrukerAktørId());

        var mellomregning = new HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>>();

        aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .filter(it -> ArbeidType.AA_REGISTER_TYPER.contains(it.getArbeidType()))
            .forEach(yrkesaktivitet -> mapYrkesAktivitet(mellomregning, yrkesaktivitet));

        return utledBortfallendeAktiviteterSomSkalFortsattKompenseres(mellomregning, ikkeAktivTidslinje);
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> utledBortfallendeAktiviteterSomSkalFortsattKompenseres(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> mellomregning, LocalDateTimeline<Boolean> ikkeAktivTidslinje) {
        var resultat = new HashMap<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>();
        for (LocalDateSegment<Boolean> periodeMedYtelse : ikkeAktivTidslinje.toSegments()) {
            for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> aktivitet : mellomregning.entrySet()) {
                var arbeidsgiverTidslinje = aktivitet.getValue();
                var yrkesaktivIPeriodeMedYtelse = arbeidsgiverTidslinje.intersection(periodeMedYtelse.getLocalDateInterval());

                var ikkeAktivPeriode = new LocalDateTimeline<>(List.of(periodeMedYtelse)).disjoint(yrkesaktivIPeriodeMedYtelse);

                if (!ikkeAktivPeriode.isEmpty()) {
                    if (ikkeAktivPeriode.toSegments().stream().noneMatch(it -> Objects.equals(it.getFom(), periodeMedYtelse.getFom()))) {
                        var segmenter = ikkeAktivPeriode.toSegments()
                            .stream()
                            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(),
                                new WrappedArbeid(new ArbeidPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()), UttakArbeidType.IKKE_YRKESAKTIV, aktivitet.getKey().getArbeidsgiver(), null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO))))
                            .collect(Collectors.toList());
                        resultat.put(aktivitet.getKey(), new LocalDateTimeline<>(segmenter));
                    }
                }
            }
        }
        return resultat;
    }

    private void mapYrkesAktivitet(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> resultat, Yrkesaktivitet yrkesaktivitet) {
        var key = utledIdentifikator(yrkesaktivitet);
        var arbeidsAktivTidslinje = resultat.getOrDefault(key, new LocalDateTimeline<>(List.of()));

        var segmenter = yrkesaktivitet.getAnsettelsesPeriode().stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .collect(Collectors.toList());
        // Har ikke helt kontroll på aa-reg mtp overlapp her så better safe than sorry
        for (LocalDateSegment<Boolean> segment : segmenter) {
            var arbeidsforholdTidslinje = new LocalDateTimeline<>(List.of(segment));
            arbeidsAktivTidslinje = arbeidsAktivTidslinje.combine(arbeidsforholdTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        resultat.put(key, arbeidsAktivTidslinje.compress());
    }

    private AktivitetIdentifikator utledIdentifikator(Yrkesaktivitet yrkesaktivitet) {
        return new AktivitetIdentifikator(UttakArbeidType.IKKE_YRKESAKTIV, yrkesaktivitet.getArbeidsgiver(), null);
    }
}
