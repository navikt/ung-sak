package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class MapOppgittFraværOgVilkårsResultat {
    public MapOppgittFraværOgVilkårsResultat() {
    }

    Map<Aktivitet, List<WrappedOppgittFraværPeriode>> utledPerioderMedUtfallHvisAvslåttVilkår(OppgittFravær grunnlag, Vilkårene vilkårene) {

        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje = opprettFraværsTidslinje(grunnlag);
        LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje = opprettVilkårTidslinje(vilkårene);

        return kombinerTidslinjene(fraværsTidslinje, avslåtteVilkårTidslinje);
    }

    private Map<Aktivitet, List<WrappedOppgittFraværPeriode>> kombinerTidslinjene(Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje,
                                                                                 LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje) {
        Map<Aktivitet, List<WrappedOppgittFraværPeriode>> result = new HashMap<>();

        for (Map.Entry<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> entry : fraværsTidslinje.entrySet()) {
            var timeline = entry.getValue().combine(avslåtteVilkårTidslinje, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();

            result.put(entry.getKey(), timeline.toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toList()));
        }

        return result;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> opprettVilkårTidslinje(Vilkårene vilkårene) {
        var avslåtteVilkårsPerioder = vilkårene.getVilkårene()
            .stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()))
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new WrappedOppgittFraværPeriode(null, true)))
            .collect(Collectors.toList());
        LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje = new LocalDateTimeline<>(List.of());
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : avslåtteVilkårsPerioder) {
            avslåtteVilkårTidslinje = avslåtteVilkårTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return avslåtteVilkårTidslinje;
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> opprettFraværsTidslinje(OppgittFravær grunnlag) {
        var perioderPerAktivitet = grunnlag.getPerioder()
            .stream()
            .map(it -> new WrappedOppgittFraværPeriode(it, false))
            .collect(Collectors.groupingBy(WrappedOppgittFraværPeriode::getAktivitet, Collectors.toList()));

        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> result = new HashMap<>();

        for (Map.Entry<Aktivitet, List<WrappedOppgittFraværPeriode>> aktivitetsPerioder : perioderPerAktivitet.entrySet()) {
            LocalDateTimeline<WrappedOppgittFraværPeriode> fraværsTidslinje = new LocalDateTimeline<WrappedOppgittFraværPeriode>(List.of());
            for (WrappedOppgittFraværPeriode periode : aktivitetsPerioder.getValue()) {
                LocalDateSegment<WrappedOppgittFraværPeriode> segment = new LocalDateSegment<>(periode.getPeriode().getFom(), periode.getPeriode().getTom(), periode);
                fraværsTidslinje = fraværsTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
            result.put(aktivitetsPerioder.getKey(), fraværsTidslinje.compress());
        }
        return result;
    }

    private WrappedOppgittFraværPeriode opprettHoldKonsistens(LocalDateSegment<WrappedOppgittFraværPeriode> segment) {
        var segmentValue = segment.getValue();
        var oppgittPeriode = segmentValue.getPeriode();
        return new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(segment.getFom(), segment.getTom(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag()), segmentValue.getErAvslått());
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> mergePeriode(LocalDateInterval di,
                                                                       LocalDateSegment<WrappedOppgittFraværPeriode> førsteVersjon,
                                                                       LocalDateSegment<WrappedOppgittFraværPeriode> sisteVersjon) {
        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }
        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();
        if (første.getErAvslått() && !siste.getErAvslått()) {
            return lagSegment(di, første.getErAvslått(), utledOppgittPeriode(første.getPeriode(), siste.getPeriode()));
        } else if (!første.getErAvslått() && siste.getErAvslått()) {
            return lagSegment(di, siste.getErAvslått(), utledOppgittPeriode(siste.getPeriode(), første.getPeriode()));
        } else {
            return sisteVersjon;
        }
    }

    private OppgittFraværPeriode utledOppgittPeriode(OppgittFraværPeriode a, OppgittFraværPeriode b) {
        if (a == null) {
            return b;
        }
        return a;
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, boolean erAvslått, OppgittFraværPeriode oppgittPeriode) {
        var oppdaterOppgittFravær = new OppgittFraværPeriode(di.getFomDato(), di.getTomDato(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag());
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, erAvslått);
        return new LocalDateSegment<>(di, wrapper);
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, WrappedOppgittFraværPeriode segmentValue) {
        var oppgittPeriode = segmentValue.getPeriode();
        var oppdaterOppgittFravær = oppgittPeriode != null ? new OppgittFraværPeriode(di.getFomDato(), di.getTomDato(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag()) : null;
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, segmentValue.getErAvslått());
        return new LocalDateSegment<>(di, wrapper);
    }
}
