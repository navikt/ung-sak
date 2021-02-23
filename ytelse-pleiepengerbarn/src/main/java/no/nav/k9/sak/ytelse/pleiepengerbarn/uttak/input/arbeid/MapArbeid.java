package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

public class MapArbeid {

    public List<Arbeid> map(Set<KravDokument> kravDokumenter,
                            Set<PerioderFraSøknad> perioderFraSøknader) {
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
                            var key = new AktivitetIdentifikator(p.getAktivitetType(), p.getArbeidsgiver(), p.getArbeidsforholdRef());
                            var perioder = arbeidsforhold.getOrDefault(key, new LocalDateTimeline<>(List.of()));
                            var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), new WrappedArbeid(p))));
                            perioder = perioder.combine(timeline, this::merge, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                            arbeidsforhold.put(key, perioder);
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
                    .toSegments().forEach(p -> {
                    var periode = p.getValue().getPeriode();
                    var jobberNormalt = Optional.ofNullable(periode.getJobberNormaltTimerPerDag()).orElse(Duration.ZERO);
                    var jobberFaktisk = Optional.ofNullable(periode.getFaktiskArbeidTimerPerDag()).orElse(Duration.ZERO);
                    perioder.put(new LukketPeriode(p.getFom(), p.getTom()),
                        new ArbeidsforholdPeriodeInfo(jobberNormalt, jobberFaktisk));
                });

                return new Arbeid(mapArbeidsforhold(arbeidPeriodes.getKey()), perioder);
            })
            .collect(Collectors.toList());
    }

    private LocalDateSegment<WrappedArbeid> merge(LocalDateInterval di, LocalDateSegment<WrappedArbeid> førsteVersjon, LocalDateSegment<WrappedArbeid> sisteVersjon) {
        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }
        var siste = Objects.requireNonNull(sisteVersjon).getValue();

        return lagSegment(di, siste);
    }

    private LocalDateSegment<WrappedArbeid> lagSegment(LocalDateInterval di, WrappedArbeid segmentValue) {
        var oppgittPeriode = segmentValue.getPeriode();
        var arbeidPeriode = oppgittPeriode != null ? new ArbeidPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato()), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFaktiskArbeidTimerPerDag(), oppgittPeriode.getJobberNormaltTimerPerDag()) : null;
        var wrapper = new WrappedArbeid(arbeidPeriode);
        return new LocalDateSegment<>(di, wrapper);
    }

    private Arbeidsforhold mapArbeidsforhold(AktivitetIdentifikator identifikator) {
        return new Arbeidsforhold(identifikator.getAktivitetType().getKode(),
            identifikator.getArbeidsgiver().getArbeidsgiverOrgnr(),
            Optional.ofNullable(identifikator.getArbeidsgiver().getArbeidsgiverAktørId()).map(AktørId::getId).orElse(null),
            Optional.ofNullable(identifikator.getArbeidsforhold()).map(InternArbeidsforholdRef::getReferanse).orElse(null)
        );
    }
}
