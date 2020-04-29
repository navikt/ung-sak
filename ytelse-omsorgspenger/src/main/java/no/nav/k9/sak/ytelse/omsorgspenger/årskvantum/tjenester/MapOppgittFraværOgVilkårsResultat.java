package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class MapOppgittFraværOgVilkårsResultat {
    public MapOppgittFraværOgVilkårsResultat() {
    }

    Map<Aktivitet, Set<WrappedOppgittFraværPeriode>> utledPerioderMedUtfallHvisAvslåttVilkår(OppgittFravær grunnlag, Vilkårene vilkårene) {

        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje = opprettFraværsTidslinje(grunnlag);
        LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje = opprettVilkårTidslinje(vilkårene);

        return kombinerTidslinjene(fraværsTidslinje, avslåtteVilkårTidslinje);
    }

    private Map<Aktivitet, Set<WrappedOppgittFraværPeriode>> kombinerTidslinjene(Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje,
                                                                                 LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje) {
        Map<Aktivitet, Set<WrappedOppgittFraværPeriode>> result = new HashMap<>();

        for (Map.Entry<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> entry : fraværsTidslinje.entrySet()) {
            var timeline = entry.getValue().combine(avslåtteVilkårTidslinje, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();

            result.put(entry.getKey(), timeline.toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toSet()));
        }

        return result;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> opprettVilkårTidslinje(Vilkårene vilkårene) {
        var avslåtteVilkårsPerioder = vilkårene.getVilkårene()
            .stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()))
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new WrappedOppgittFraværPeriode(null, mapToVurderteVilkår(it.getVilkårType(),it.getGjeldendeUtfall()))))
            .collect(Collectors.toList());
        LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje = new LocalDateTimeline<>(List.of());
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : avslåtteVilkårsPerioder) {
            avslåtteVilkårTidslinje = avslåtteVilkårTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return avslåtteVilkårTidslinje;
    }

    private Map<no.nav.k9.aarskvantum.kontrakter.Vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall> mapToVurderteVilkår(VilkårType vilkårType, Utfall utfall) {
        Map<no.nav.k9.aarskvantum.kontrakter.Vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall> vurderteVilkår = new HashMap<>();

        //TODO finn kode for medlemskap og opptjening
        if (Utfall.OPPFYLT.equals(utfall)) {
            vurderteVilkår.put(no.nav.k9.aarskvantum.kontrakter.Vilkår.OPPTJENINGSVILKÅR, no.nav.k9.aarskvantum.kontrakter.Utfall.INNVILGET);
        } else {
            vurderteVilkår.put(no.nav.k9.aarskvantum.kontrakter.Vilkår.OPPTJENINGSVILKÅR, no.nav.k9.aarskvantum.kontrakter.Utfall.AVSLÅTT);
        }
        return vurderteVilkår;
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> opprettFraværsTidslinje(OppgittFravær grunnlag) {
        var perioderPerAktivitet = grunnlag.getPerioder()
            .stream()
            .map(it -> new WrappedOppgittFraværPeriode(it, new HashMap<>()))
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
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag()), segmentValue.getVurderteVilkår());
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
        if (første.erAvslått() && !siste.erAvslått()) {
            return lagSegment(di, utledOppgittPeriode(første.getPeriode(), siste.getPeriode()), første.getVurderteVilkår());
        } else if (!første.erAvslått() && siste.erAvslått()) {
            return lagSegment(di, utledOppgittPeriode(siste.getPeriode(), første.getPeriode()), siste.getVurderteVilkår());
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

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, OppgittFraværPeriode oppgittPeriode, Map<no.nav.k9.aarskvantum.kontrakter.Vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall> vurderteVilkår) {
        var oppdaterOppgittFravær = new OppgittFraværPeriode(di.getFomDato(), di.getTomDato(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag());
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, vurderteVilkår);
        return new LocalDateSegment<>(di, wrapper);
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, WrappedOppgittFraværPeriode segmentValue) {
        var oppgittPeriode = segmentValue.getPeriode();
        var oppdaterOppgittFravær = oppgittPeriode != null ? new OppgittFraværPeriode(di.getFomDato(), di.getTomDato(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag()) : null;
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, segmentValue.getVurderteVilkår());
        return new LocalDateSegment<>(di, wrapper);
    }
}
