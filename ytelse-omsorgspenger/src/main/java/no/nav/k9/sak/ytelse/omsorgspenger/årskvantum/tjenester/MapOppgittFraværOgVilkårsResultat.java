package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class MapOppgittFraværOgVilkårsResultat {
    public MapOppgittFraværOgVilkårsResultat() {
    }

    Map<Aktivitet, List<WrappedOppgittFraværPeriode>> utledPerioderMedUtfall(BehandlingReferanse ref, OppgittFravær grunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag, Vilkårene vilkårene) {
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId()));

        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje = opprettFraværsTidslinje(grunnlag);
        Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdOgPermitertTidslinje = opprettPermitertTidslinje(filter);
        fraværsTidslinje = kombinerTidslinjer(fraværsTidslinje, arbeidsforholdOgPermitertTidslinje);
        LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje = opprettVilkårTidslinje(vilkårene);

        return kombinerTidslinjene(fraværsTidslinje, avslåtteVilkårTidslinje);
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> kombinerTidslinjer(Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje,
                                                                                              Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdOgPermitertTidslinje) {
        var result = new HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>>();
        for (Aktivitet aktivitet : fraværsTidslinje.keySet()) {
            var arbeidsforholdSomMatcher = arbeidsforholdOgPermitertTidslinje.keySet()
                .stream()
                .filter(it -> it.matcher(aktivitet))
                .map(arbeidsforholdOgPermitertTidslinje::get)
                .collect(Collectors.toList());
            result.put(aktivitet, mergeTidslinjer(fraværsTidslinje.get(aktivitet), arbeidsforholdSomMatcher));
        }
        return result;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> mergeTidslinjer(LocalDateTimeline<WrappedOppgittFraværPeriode> wrappedOppgittFraværPeriodeLocalDateTimeline,
                                                                           List<LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdSomMatcher) {
        var tidslinje = wrappedOppgittFraværPeriodeLocalDateTimeline;
        var arbeidsforholdTidslinje = mergeTidslinjer(arbeidsforholdSomMatcher);
        tidslinje = tidslinje.combine(arbeidsforholdTidslinje, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return tidslinje.compress();
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> mergeTidslinjer(List<LocalDateTimeline<WrappedOppgittFraværPeriode>> arbeidsforholdSomMatcher) {
        var tidslinje = new LocalDateTimeline<WrappedOppgittFraværPeriode>(List.of());
        for (LocalDateTimeline<WrappedOppgittFraværPeriode> oppgittFraværPeriodeLocalDateTimeline : arbeidsforholdSomMatcher) {
            tidslinje = tidslinje.combine(oppgittFraværPeriodeLocalDateTimeline, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return tidslinje.compress();
    }

    private Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> opprettPermitertTidslinje(YrkesaktivitetFilter filter) {
        var result = new HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>>();

        filter.getYrkesaktiviteter().forEach(ya -> mapYaTilTidlinje(ya, result, filter));

        return result;
    }

    private void mapYaTilTidlinje(Yrkesaktivitet yrkesaktivitet, HashMap<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> result, YrkesaktivitetFilter filter) {
        var tidlinje = opprettArbeidsforholdTidslinje(yrkesaktivitet, filter);
        result.put(new Aktivitet(yrkesaktivitet.getArbeidsgiver(), yrkesaktivitet.getArbeidsforholdRef()), tidlinje.compress());
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> opprettArbeidsforholdTidslinje(Yrkesaktivitet yrkesaktivitet, YrkesaktivitetFilter filter) {
        LocalDateTimeline<WrappedOppgittFraværPeriode> allVerdenAvTid = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE, new WrappedOppgittFraværPeriode(null, null, true, null))));
        var ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet).stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), new WrappedOppgittFraværPeriode(null, null, false, null)))
            .collect(Collectors.toList());
        var permisjonsPerioder = yrkesaktivitet.getPermisjon()
            .stream()
            .filter(it -> erStørreEllerLik100Prosent(it.getProsentsats()))
            .map(it -> new LocalDateSegment<>(it.getFraOgMed(), it.getTilOgMed(), new WrappedOppgittFraværPeriode(null, true, null, null)))
            .collect(Collectors.toList());

        LocalDateTimeline<WrappedOppgittFraværPeriode> arbeidsforholdTidslinje = allVerdenAvTid;
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : ansettelsesPerioder) {
            arbeidsforholdTidslinje = arbeidsforholdTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        for (LocalDateSegment<WrappedOppgittFraværPeriode> segment : permisjonsPerioder) {
            arbeidsforholdTidslinje = arbeidsforholdTidslinje.combine(new LocalDateTimeline<>(List.of(segment)), this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return arbeidsforholdTidslinje;
    }

    private boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }

    private Map<Aktivitet, List<WrappedOppgittFraværPeriode>> kombinerTidslinjene(Map<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsTidslinje,
                                                                                  LocalDateTimeline<WrappedOppgittFraværPeriode> avslåtteVilkårTidslinje) {
        Map<Aktivitet, List<WrappedOppgittFraværPeriode>> result = new HashMap<>();

        for (Map.Entry<Aktivitet, LocalDateTimeline<WrappedOppgittFraværPeriode>> entry : fraværsTidslinje.entrySet()) {
            var timeline = entry.getValue().combine(avslåtteVilkårTidslinje, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN).compress();

            result.put(entry.getKey(), timeline.map(this::splittDelevisFravær)
                .toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toList()));
        }

        return result;
    }

    private List<LocalDateSegment<WrappedOppgittFraværPeriode>> splittDelevisFravær(LocalDateSegment<WrappedOppgittFraværPeriode> segment) {
        if (segment.getValue() == null) {
            return List.of();
        }
        if (segment.getValue().getPeriode() == null) {
            return List.of();
        }
        if (segment.getValue().getPeriode().getFraværPerDag() == null) {
            return List.of(segment);
        }
        var segmenter = new ArrayList<LocalDateSegment<WrappedOppgittFraværPeriode>>();
        var startDato = segment.getFom();
        var endDato = segment.getTom();
        segmenter.add(new LocalDateSegment<>(startDato, startDato, segment.getValue()));
        while (!startDato.equals(endDato)) {
            startDato = startDato.plusDays(1);
            segmenter.add(new LocalDateSegment<>(startDato, startDato, segment.getValue()));
        }

        return segmenter;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> opprettVilkårTidslinje(Vilkårene vilkårene) {
        var avslåtteVilkårsPerioder = vilkårene.getVilkårene()
            .stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()))
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new WrappedOppgittFraværPeriode(null, null, null, true)))
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
            .map(it -> new WrappedOppgittFraværPeriode(it, null, null, null))
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
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag()), segmentValue.getErIPermisjon(), segmentValue.getErIkkeIArbeid(), segmentValue.getErAvslåttInngangsvilkår());
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

        var avslåttInngangsvilkår = booleanPrioTrue(første.getErAvslåttInngangsvilkår(), siste.getErAvslåttInngangsvilkår());
        var iPermisjon = booleanPrioTrue(første.getErIPermisjon(), siste.getErIPermisjon());
        var ikkeIArbeid = booleanPrioFalse(første.getErIkkeIArbeid(), siste.getErIkkeIArbeid());

        return lagSegment(di, avslåttInngangsvilkår, utledOppgittPeriode(første.getPeriode(), siste.getPeriode()), iPermisjon, ikkeIArbeid);
    }

    private Boolean booleanPrioFalse(Boolean boolOne, Boolean boolTwo) {
        if (boolOne == null || boolTwo == null) {
            return boolTwo != null ? boolTwo : boolOne;
        }
        return boolOne && boolTwo;
    }

    private Boolean booleanPrioTrue(Boolean boolOne, Boolean boolTwo) {
        if (boolOne == null || boolTwo == null) {
            return boolTwo != null ? boolTwo : boolOne;
        }
        return boolOne || boolTwo;
    }

    private OppgittFraværPeriode utledOppgittPeriode(OppgittFraværPeriode a, OppgittFraværPeriode b) {
        if (a == null) {
            return b;
        }
        return a;
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, Boolean erAvslått, OppgittFraværPeriode oppgittPeriode, Boolean iPermisjon, Boolean ikkeIArbeid) {
        var oppdaterOppgittFravær = oppgittPeriode != null ? new OppgittFraværPeriode(di.getFomDato(), di.getTomDato(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag()) : null;
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, iPermisjon, ikkeIArbeid, erAvslått);
        return new LocalDateSegment<>(di, wrapper);
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> lagSegment(LocalDateInterval di, WrappedOppgittFraværPeriode segmentValue) {
        var oppgittPeriode = segmentValue.getPeriode();
        var oppdaterOppgittFravær = oppgittPeriode != null ? new OppgittFraværPeriode(di.getFomDato(), di.getTomDato(), oppgittPeriode.getAktivitetType(),
            oppgittPeriode.getArbeidsgiver(), oppgittPeriode.getArbeidsforholdRef(), oppgittPeriode.getFraværPerDag()) : null;
        var wrapper = new WrappedOppgittFraværPeriode(oppdaterOppgittFravær, segmentValue.getErIPermisjon(), segmentValue.getErIkkeIArbeid(), segmentValue.getErAvslåttInngangsvilkår());
        return new LocalDateSegment<>(di, wrapper);
    }
}
