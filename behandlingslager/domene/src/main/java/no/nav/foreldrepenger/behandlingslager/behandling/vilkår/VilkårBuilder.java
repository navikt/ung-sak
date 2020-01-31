package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class VilkårBuilder {

    private final Vilkår vilkåret;
    private LocalDateTimeline<VilkårPeriode> vilkårTidslinje;
    private boolean bygget = false;

    public VilkårBuilder() {
        this.vilkåret = new Vilkår();
        this.vilkårTidslinje = new LocalDateTimeline<>(List.of());
    }

    VilkårBuilder(Vilkår vilkåret) {
        this.vilkåret = new Vilkår(vilkåret);
        this.vilkårTidslinje = new LocalDateTimeline<>(vilkåret.getPerioder().stream().map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a)).collect(Collectors.toList()));
    }

    public VilkårBuilder medType(VilkårType type) {
        validerBuilder();
        vilkåret.setVilkårType(type);
        return this;
    }

    public VilkårBuilder leggTil(VilkårPeriodeBuilder periodeBuilder) {
        validerBuilder();
        final var periode = periodeBuilder.build();
        final var segment = new LocalDateSegment<>(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato(), periode);
        final var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

        this.vilkårTidslinje = vilkårTidslinje.combine(periodeTidslinje, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return this;
    }

    private LocalDateSegment<VilkårPeriode> sjekkVurdering(LocalDateInterval di,
                                                           LocalDateSegment<VilkårPeriode> førsteVersjon,
                                                           LocalDateSegment<VilkårPeriode> sisteVersjon) {

        if (førsteVersjon == null && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return lagSegment(di, førsteVersjon.getValue());
        }

        VilkårPeriode første = førsteVersjon.getValue();
        VilkårPeriode siste = sisteVersjon.getValue();

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

    private LocalDateSegment<VilkårPeriode> lagSegment(LocalDateInterval di, VilkårPeriode siste) {
        VilkårPeriodeBuilder builder = new VilkårPeriodeBuilder(siste);
        VilkårPeriode aktivitetPeriode = builder.medPeriode(di.getFomDato(), di.getTomDato()).build();
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }

    Vilkår build() {
        validerBuilder();
        bygget = true;
        final var collect = vilkårTidslinje.compress()
            .toSegments()
            .stream()
            .map(this::opprettHoldKonsistens)
            .collect(Collectors.toList());
        vilkåret.setPerioder(collect);
        return vilkåret;
    }

    private VilkårPeriode opprettHoldKonsistens(LocalDateSegment<VilkårPeriode> segment) {
        return new VilkårPeriodeBuilder(segment.getValue())
            .medPeriode(segment.getFom(), segment.getTom())
            .build();
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
        return new VilkårPeriodeBuilder(intersection.getValue())
            .medPeriode(fom, tom);
    }
}
