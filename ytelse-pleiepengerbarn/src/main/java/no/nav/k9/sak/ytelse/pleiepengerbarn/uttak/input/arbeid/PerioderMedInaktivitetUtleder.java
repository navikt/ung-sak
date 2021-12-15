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
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Stillingsprosent;
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
                var arbeidsforholdUtenStartPåStp = aktivitet.getValue()
                    .toSegments()
                    .stream()
                    .filter(it -> !Objects.equals(it.getFom(), periodeMedYtelse.getFom()))
                    .collect(Collectors.toList());
                var arbeidsgiverTidslinje = new LocalDateTimeline<>(arbeidsforholdUtenStartPåStp);

                var justertSegment = new LocalDateSegment<>(periodeMedYtelse.getFom().minusDays(1), periodeMedYtelse.getTom(), periodeMedYtelse.getValue());
                var yrkesaktivIPeriodeMedYtelse = arbeidsgiverTidslinje.intersection(justertSegment.getLocalDateInterval());

                if (yrkesaktivIPeriodeMedYtelse.isEmpty()) {
                    continue;
                }

                var ikkeAktivPeriode = new LocalDateTimeline<>(List.of(justertSegment)).disjoint(yrkesaktivIPeriodeMedYtelse);

                if (ikkeAktivPeriode.isEmpty()) {
                    continue;
                }

                if (ikkeAktivPeriode.toSegments().stream().noneMatch(it -> Objects.equals(it.getFom(), justertSegment.getFom()))) {
                    var segmenter = ikkeAktivPeriode.toSegments()
                        .stream()
                        .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(),
                            new WrappedArbeid(new ArbeidPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()), UttakArbeidType.IKKE_YRKESAKTIV, aktivitet.getKey().getArbeidsgiver(), null, Duration.ofMinutes((long) (7.5 * 60)), Duration.ZERO))))
                        .collect(Collectors.toList());
                    resultat.put(aktivitet.getKey(), new LocalDateTimeline<>(segmenter));
                }
            }
        }
        return resultat;
    }

    private void mapYrkesAktivitet(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> resultat, Yrkesaktivitet yrkesaktivitet) {
        var key = utledIdentifikator(yrkesaktivitet);
        var arbeidsAktivTidslinje = resultat.getOrDefault(key, new LocalDateTimeline<>(List.of()));

        var segmenter = yrkesaktivitet.getAnsettelsesPeriode()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .toList();
        // Har ikke helt kontroll på aa-reg mtp overlapp her så better safe than sorry
        for (LocalDateSegment<Boolean> segment : segmenter) {
            var arbeidsforholdTidslinje = new LocalDateTimeline<>(List.of(segment));
            arbeidsAktivTidslinje = arbeidsAktivTidslinje.combine(arbeidsforholdTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        // Ta bort permisjoner
        var permitteringsTidslinje = mapPermittering(yrkesaktivitet);
        arbeidsAktivTidslinje = arbeidsAktivTidslinje.disjoint(permitteringsTidslinje);

        resultat.put(key, arbeidsAktivTidslinje.compress());
    }

    private LocalDateTimeline<Boolean> mapPermittering(Yrkesaktivitet yrkesaktivitet) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());

        var relevantePermitteringer = yrkesaktivitet.getPermisjon().stream()
            .filter(it -> Objects.equals(it.getPermisjonsbeskrivelseType(), PermisjonsbeskrivelseType.PERMITTERING))
            .filter(it -> erStørreEllerLik100Prosent(it.getProsentsats()))
            .toList();

        for (Permisjon permisjon : relevantePermitteringer) {
            var permittert = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(permisjon.getFraOgMed(), permisjon.getTilOgMed(), true)));
            timeline = timeline.combine(permittert, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return timeline.compress();
    }

    private AktivitetIdentifikator utledIdentifikator(Yrkesaktivitet yrkesaktivitet) {
        return new AktivitetIdentifikator(UttakArbeidType.IKKE_YRKESAKTIV, yrkesaktivitet.getArbeidsgiver(), null);
    }

    private boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }
}
