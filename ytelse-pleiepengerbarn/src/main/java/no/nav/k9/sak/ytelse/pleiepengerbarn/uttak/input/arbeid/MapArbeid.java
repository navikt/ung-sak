package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.PsbInntektsmeldingerRelevantForBeregning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapArbeid {

    private PsbInntektsmeldingerRelevantForBeregning inntektsmeldingerRelevantForBeregning = new PsbInntektsmeldingerRelevantForBeregning();

    public List<Arbeid> map(TreeSet<KravDokument> kravDokumenter,
                            Set<PerioderFraSøknad> perioderFraSøknader,
                            LocalDateTimeline<Boolean> tidslinjeTilVurdering,
                            Set<Inntektsmelding> sakInntektsmeldinger) {

        final Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> arbeidsforhold = new HashMap<>();

        kravDokumenter.stream()
            .sorted()
            .forEachOrdered(at -> {
                var dokumenter = perioderFraSøknader.stream()
                    .filter(it -> it.getJournalpostId().equals(at.getJournalpostId()))
                    .collect(Collectors.toSet());
                if (dokumenter.size() == 1) {
                    dokumenter.stream()
                        .map(PerioderFraSøknad::getArbeidPerioder)
                        .flatMap(Collection::stream)
                        .forEach(p -> {
                            var keys = utledRelevanteKeys(p, tidslinjeTilVurdering, sakInntektsmeldinger);
                            for (AktivitetIdentifikator key : keys) {
                                var perioder = arbeidsforhold.getOrDefault(key, new LocalDateTimeline<>(List.of()));
                                var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), new WrappedArbeid(p))));
                                perioder = perioder.combine(timeline, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                                arbeidsforhold.put(key, perioder);
                            }
                        });
                } else {
                    throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + at);
                }
            });

        return arbeidsforhold.entrySet()
            .stream()
            .map(arbeidPeriodes -> {
                var perioder = new HashMap<LukketPeriode, ArbeidsforholdPeriodeInfo>();
                arbeidPeriodes.getValue()
                    .compress()
                    .intersection(tidslinjeTilVurdering)
                    .toSegments()
                    .forEach(p -> {
                        var periode = p.getValue().getPeriode();
                        var antallLinjerPerArbeidsgiver = arbeidsforhold.keySet().stream().filter(it -> it.getArbeidsgiver().equals(periode.getArbeidsgiver())).count();
                        var jobberNormalt = justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Optional.ofNullable(periode.getJobberNormaltTimerPerDag()).orElse(justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Duration.ZERO)));
                        var jobberFaktisk = justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Optional.ofNullable(periode.getFaktiskArbeidTimerPerDag()).orElse(justerIHenholdTilAntallet(antallLinjerPerArbeidsgiver, Duration.ZERO)));
                        perioder.put(new LukketPeriode(p.getFom(), p.getTom()),
                            new ArbeidsforholdPeriodeInfo(jobberNormalt, jobberFaktisk));
                    });

                return new Arbeid(mapArbeidsforhold(arbeidPeriodes.getKey()), perioder);
            })
            .collect(Collectors.toList());
    }

    private Duration justerIHenholdTilAntallet(long antallLinjerPerArbeidsgiver, Duration duration) {
        if (Duration.ZERO.equals(duration) || antallLinjerPerArbeidsgiver == 0 || antallLinjerPerArbeidsgiver == 1) {
            return duration;
        }
        return duration.dividedBy(antallLinjerPerArbeidsgiver);
    }

    private Set<AktivitetIdentifikator> utledRelevanteKeys(ArbeidPeriode p,
                                                           LocalDateTimeline<Boolean> tidslinjeTilVurdering,
                                                           Set<Inntektsmelding> sakInntektsmeldinger) {
        var segment = tidslinjeTilVurdering.getSegment(new LocalDateInterval(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()));
        var arbeidsforholdRefs = inntektsmeldingerRelevantForBeregning.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
            .stream()
            .filter(it -> it.getArbeidsgiver().equals(p.getArbeidsgiver()))
            .map(Inntektsmelding::getArbeidsforholdRef)
            .collect(Collectors.toSet());

        if (arbeidsforholdRefs.isEmpty()) {
            var key = new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), p.getArbeidsforholdRef());
            return Set.of(key);
        } else {
            return arbeidsforholdRefs.stream()
                .map(it -> new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), it))
                .collect(Collectors.toSet());
        }
    }

    private Arbeidsforhold mapArbeidsforhold(AktivitetIdentifikator identifikator) {
        return new Arbeidsforhold(identifikator.getAktivitetType().getKode(),
            identifikator.getArbeidsgiver().getArbeidsgiverOrgnr(),
            Optional.ofNullable(identifikator.getArbeidsgiver().getArbeidsgiverAktørId()).map(AktørId::getId).orElse(null),
            Optional.ofNullable(identifikator.getArbeidsforhold()).map(InternArbeidsforholdRef::getReferanse).orElse(null)
        );
    }
}
