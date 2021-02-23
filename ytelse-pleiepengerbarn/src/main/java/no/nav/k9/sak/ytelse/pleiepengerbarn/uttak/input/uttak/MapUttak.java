package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.uttak;

import java.util.List;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.AktivitetIdentifikator;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.SøktUttak;

public class MapUttak {

    public List<SøktUttak> map(Set<KravDokument> kravDokumenter,
                               Set<PerioderFraSøknad> perioderFraSøknader) {
        var resultatTimeline = new LocalDateTimeline<WrappedUttak>(List.of());
        for (KravDokument kravDokument : kravDokumenter) {
            var dokumenter = perioderFraSøknader.stream()
                .filter(it -> it.getJournalpostId().equals(kravDokument.getJournalpostId()))
                .collect(Collectors.toSet());
            if (dokumenter.size() == 1) {
                var perioderFraSøknad = dokumenter.iterator().next();
                for (var periode : perioderFraSøknad.getUttakPerioder()) {
                    var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new WrappedUttak(periode))));
                    resultatTimeline = resultatTimeline.combine(timeline, this::merge, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            } else {
                throw new IllegalStateException("Fant " + dokumenter.size() + " for dokumentet : " + dokumenter);
            }
        }

        return resultatTimeline.compress()
            .toSegments()
            .stream()
            .map(it -> new SøktUttak(new LukketPeriode(it.getFom(), it.getTom()), it.getValue().getPeriode().getTimerPleieAvBarnetPerDag()))
            .collect(Collectors.toList());
    }

    private LocalDateSegment<WrappedUttak> merge(LocalDateInterval di, LocalDateSegment<WrappedUttak> førsteVersjon, LocalDateSegment<WrappedUttak> sisteVersjon) {
        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }
        var siste = Objects.requireNonNull(sisteVersjon).getValue();

        return lagSegment(di, siste);
    }

    private LocalDateSegment<WrappedUttak> lagSegment(LocalDateInterval di, WrappedUttak segmentValue) {
        var oppgittPeriode = segmentValue.getPeriode();
        var arbeidPeriode = oppgittPeriode != null ? new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(di.getFomDato(), di.getTomDato()), oppgittPeriode.getTimerPleieAvBarnetPerDag()) : null;
        var wrapper = new WrappedUttak(arbeidPeriode);
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
