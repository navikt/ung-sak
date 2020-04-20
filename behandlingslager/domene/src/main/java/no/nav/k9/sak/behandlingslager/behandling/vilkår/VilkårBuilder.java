package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VilkårBuilder {

    private final Vilkår vilkåret;
    private LocalDateTimeline<WrappedVilkårPeriode> vilkårTidslinje;
    private boolean bygget = false;
    private int mellomliggendePeriodeAvstand = 0;

    public VilkårBuilder() {
        this.vilkåret = new Vilkår();
        this.vilkårTidslinje = new LocalDateTimeline<>(List.of());
    }

    VilkårBuilder(Vilkår vilkåret) {
        this.vilkåret = new Vilkår(vilkåret);
        this.vilkårTidslinje = new LocalDateTimeline<>(vilkåret.getPerioder().stream().map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), new WrappedVilkårPeriode(a))).collect(Collectors.toList()));
    }

    boolean erMellomliggendePeriode(LocalDate firstDate, LocalDate secondDate) {
        final long avstand;
        if (firstDate.isBefore(secondDate)) {
            avstand = ChronoUnit.DAYS.between(firstDate, secondDate);
        } else {
            avstand = ChronoUnit.DAYS.between(secondDate, firstDate);
        }
        return avstand > 0 && avstand < mellomliggendePeriodeAvstand;
    }

    public VilkårBuilder medType(VilkårType type) {
        validerBuilder();
        vilkåret.setVilkårType(type);
        return this;
    }

    public VilkårBuilder medMaksMellomliggendePeriodeAvstand(int mellomliggendePeriodeAvstand) {
        if (mellomliggendePeriodeAvstand < 0) {
            throw new IllegalArgumentException("Må være positivt");
        }
        this.mellomliggendePeriodeAvstand = mellomliggendePeriodeAvstand;
        return this;
    }

    public VilkårBuilder leggTil(VilkårPeriodeBuilder periodeBuilder) {
        validerBuilder();
        var periode = periodeBuilder.build();
        var segment = new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), new WrappedVilkårPeriode(periode));
        var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        this.vilkårTidslinje = vilkårTidslinje.combine(periodeTidslinje, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return this;
    }

    public VilkårBuilder tilbakestill(DatoIntervallEntitet periode) {
        validerBuilder();
        var segment = new LocalDateSegment<WrappedVilkårPeriode>(periode.getFomDato(), periode.getTomDato(), null);
        var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        this.vilkårTidslinje = vilkårTidslinje.combine(periodeTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        return this;
    }

    private LocalDateSegment<WrappedVilkårPeriode> sjekkVurdering(LocalDateInterval di,
                                                                  LocalDateSegment<WrappedVilkårPeriode> førsteVersjon,
                                                                  LocalDateSegment<WrappedVilkårPeriode> sisteVersjon) {

        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        var første = førsteVersjon.getValue();
        var siste = sisteVersjon.getValue();

        // TODO: Er det rett å prioriterer overstyrte vilkårsperioder?
        if (første.getErOverstyrt() && siste.getErOverstyrt()) {
            // Begge er overstyrt så ta siste
            return sisteVersjon;
        } else if (!første.getErOverstyrt() && siste.getErOverstyrt()) {
            return lagSegment(di, siste);
        } else if (første.getErOverstyrt() && !siste.getErOverstyrt()) {
            return lagSegment(di, første);
        } else {
            return sisteVersjon;
        }
    }

    private LocalDateSegment<WrappedVilkårPeriode> lagSegment(LocalDateInterval di, WrappedVilkårPeriode siste) {
        VilkårPeriodeBuilder builder = new VilkårPeriodeBuilder(siste.getVilkårPeriode());
        var aktivitetPeriode = new WrappedVilkårPeriode(builder.medPeriode(di.getFomDato(), di.getTomDato()).build());
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }

    private void kobleSammenMellomliggendeVilkårsPerioder() {
        final var mellomliggendeSegmenter = new ArrayList<DatoIntervallEntitet>();
        LocalDate tom = null;
        for (LocalDateSegment<WrappedVilkårPeriode> periode : vilkårTidslinje.toSegments()) {
            if (tom != null && erMellomliggendePeriode(tom, periode.getFom())) {
                mellomliggendeSegmenter.add(DatoIntervallEntitet.fraOgMedTilOgMed(tom, periode.getFom().minusDays(1)));
            }
            tom = periode.getTom();
        }

        mellomliggendeSegmenter.forEach(it -> this.leggTil(this.hentBuilderFor(it.getFomDato(), it.getTomDato())));
    }

    private WrappedVilkårPeriode opprettHoldKonsistens(LocalDateSegment<WrappedVilkårPeriode> segment) {
        return new WrappedVilkårPeriode(new VilkårPeriodeBuilder(segment.getValue().getVilkårPeriode())
            .medPeriode(segment.getFom(), segment.getTom())
            .build());
    }

    private void validerBuilder() {
        if (bygget) {
            throw new IllegalStateException("Skal ikke gjenbruke builders");
        }
    }

    public VilkårPeriodeBuilder hentBuilderFor(LocalDate fom, LocalDate tom) {
        validerBuilder();
        final var intersection = vilkårTidslinje.getSegment(new LocalDateInterval(fom, tom));
        if (intersection == null) {
            return new VilkårPeriodeBuilder()
                .medPeriode(fom, tom);
        }
        return new VilkårPeriodeBuilder(intersection.getValue().getVilkårPeriode())
            .medPeriode(fom, tom);
    }

    public VilkårPeriodeBuilder hentBuilderFor(DatoIntervallEntitet periode) {
        return hentBuilderFor(periode.getFomDato(), periode.getTomDato());
    }

    /**
     * Benyttes utenfor repository kun for testing
     *
     * @return vilkåret
     */
    public Vilkår build() {
        validerBuilder();
        if (!vilkårTidslinje.isContinuous()) {
            kobleSammenMellomliggendeVilkårsPerioder();
        }
        bygget = true;
        final var collect = vilkårTidslinje.compress()
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(this::opprettHoldKonsistens)
            .map(WrappedVilkårPeriode::getVilkårPeriode)
            .collect(Collectors.toList());
        vilkåret.setPerioder(collect);
        return vilkåret;
    }
}
