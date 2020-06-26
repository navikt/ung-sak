package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VilkårBuilder {

    private final Vilkår vilkåret;
    private final NavigableSet<DatoIntervallEntitet> tilbakestiltePerioder = new TreeSet<>();
    private KantIKantVurderer kantIKantVurderer = new DefaultKantIKantVurderer();
    private LocalDateTimeline<WrappedVilkårPeriode> vilkårTidslinje;
    private boolean bygget = false;
    private int mellomliggendePeriodeAvstand = 0;

    public VilkårBuilder() {
        this.vilkåret = new Vilkår();
        this.vilkårTidslinje = new LocalDateTimeline<>(List.of());
    }

    VilkårBuilder(Vilkår vilkåret) {
        this.vilkåret = new Vilkår(vilkåret);
        this.vilkårTidslinje = new LocalDateTimeline<>(vilkåret.getPerioder()
            .stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), new WrappedVilkårPeriode(a)))
            .collect(Collectors.toList()));
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

    public VilkårBuilder medKantIKantVurderer(KantIKantVurderer vurderer) {
        Objects.requireNonNull(vurderer);
        this.kantIKantVurderer = vurderer;
        return this;
    }

    public LocalDate getMaxDatoTilVurdering() {
        return vilkåret.getPerioder().stream()
            .map(VilkårPeriode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElse(Tid.TIDENES_ENDE);
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

        tilbakestill(new TreeSet<>(Set.of(periode)));
        return this;
    }

    public VilkårBuilder tilbakestill(NavigableSet<DatoIntervallEntitet> perioder) {
        validerBuilder();
        for (DatoIntervallEntitet periode : perioder) {
            var segment = new LocalDateSegment<WrappedVilkårPeriode>(periode.getFomDato(), periode.getTomDato(), null);
            var periodeTidslinje = new LocalDateTimeline<>(List.of(segment));

            this.vilkårTidslinje = vilkårTidslinje.combine(periodeTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        this.tilbakestiltePerioder.addAll(perioder);
        return this;
    }

    private LocalDateSegment<WrappedVilkårPeriode> sjekkVurdering(LocalDateInterval di,
                                                                  LocalDateSegment<WrappedVilkårPeriode> førsteVersjon,
                                                                  LocalDateSegment<WrappedVilkårPeriode> sisteVersjon) {

        if ((førsteVersjon == null || førsteVersjon.getValue() == null) && sisteVersjon != null) {
            return lagSegment(di, sisteVersjon.getValue());
        } else if ((sisteVersjon == null || sisteVersjon.getValue() == null) && førsteVersjon != null) {
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
        if (siste == null) {
            return new LocalDateSegment<>(di, null);
        }
        VilkårPeriodeBuilder builder = new VilkårPeriodeBuilder(siste.getVilkårPeriode());
        var aktivitetPeriode = new WrappedVilkårPeriode(builder.medPeriode(di.getFomDato(), di.getTomDato()).build());
        return new LocalDateSegment<>(di, aktivitetPeriode);
    }

    private void kobleSammenMellomliggendeVilkårsPerioder() {
        var mellomliggendeSegmenter = new TreeSet<DatoIntervallEntitet>();
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
        if (intersection == null || intersection.getValue() == null) {
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
        if (!tilbakestiltePerioder.isEmpty()) {
            justereUtfallVedTilbakestilling();
        }
        if (!vilkårTidslinje.isContinuous()) {
            kobleSammenMellomliggendeVilkårsPerioder();
        }
        if (kantIKantVurderer.erKomprimerbar()) {
            vilkårTidslinje = vilkårTidslinje.compress();
        }
        bygget = true;
        var vilkårsPerioderRaw = vilkårTidslinje
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(this::opprettHoldKonsistens)
            .map(WrappedVilkårPeriode::getVilkårPeriode)
            .collect(Collectors.toList());
        var vilkårsPerioder = sammenkobleOgJusterUtfallHvisEnPeriodeTilVurdering(vilkårsPerioderRaw);
        vilkåret.setPerioder(vilkårsPerioder);
        return vilkåret;
    }

    private void justereUtfallVedTilbakestilling() {
        var datoerSomOverlapperBakover = tilbakestiltePerioder.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .map(it -> it.minusDays(1))
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it, it))
            .collect(Collectors.toSet());
        var datoerSomOverlapperFremover = tilbakestiltePerioder.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .map(it -> it.plusDays(1))
            .map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it, it))
            .collect(Collectors.toSet());

        var datoerSomOverlapper = new HashSet<>(datoerSomOverlapperBakover);
        datoerSomOverlapper.addAll(datoerSomOverlapperFremover);

        for (DatoIntervallEntitet datoIntervallEntitet : datoerSomOverlapper) {
            if (vilkårTidslinje.intersects(new LocalDateTimeline<>(List.of(new LocalDateSegment<WrappedVilkårPeriode>(datoIntervallEntitet.getFomDato(), datoIntervallEntitet.getTomDato(), null))))) {
                var periodeBuilder = hentBuilderFor(datoIntervallEntitet)
                    .medUtfall(Utfall.IKKE_VURDERT);
                leggTil(periodeBuilder);
            }
        }
    }

    private List<VilkårPeriode> sammenkobleOgJusterUtfallHvisEnPeriodeTilVurdering(List<VilkårPeriode> vilkårsPerioderRaw) {
        var periodeTilVurdering = vilkårsPerioderRaw.stream().anyMatch(it -> Utfall.IKKE_VURDERT.equals(it.getGjeldendeUtfall()));
        var perioderSomGrenserTil = harPerioderSomIkkeErVurdertOgGrenserTilAnnenPeriode(vilkårsPerioderRaw);

        if (perioderSomGrenserTil && periodeTilVurdering) {
            VilkårPeriode periode = null;
            var vilkårPerioder = new ArrayList<VilkårPeriode>();

            for (VilkårPeriode vilkårPeriode : vilkårsPerioderRaw) {
                if (periode == null) {
                    periode = vilkårPeriode;
                } else if (kantIKantVurderer.erKantIKant(vilkårPeriode.getPeriode(), periode.getPeriode()) && enAvPeriodeneErTilVurdering(periode, vilkårPeriode)) {
                    periode = new VilkårPeriodeBuilder(periode)
                        .medPeriode(periode.getFom(), vilkårPeriode.getTom())
                        .medUtfall(Utfall.IKKE_VURDERT)
                        .medUtfallOverstyrt(Utfall.UDEFINERT)
                        .tilbakestillManuellVurdering()
                        .build();
                } else {
                    vilkårPerioder.add(periode);
                    periode = vilkårPeriode;
                }
            }
            if (periode != null) {
                vilkårPerioder.add(periode);
            }
            return vilkårPerioder;
        }
        return vilkårsPerioderRaw;
    }

    private boolean harPerioderSomIkkeErVurdertOgGrenserTilAnnenPeriode(List<VilkårPeriode> vilkårsPerioderRaw) {
        return vilkårsPerioderRaw.stream()
            .filter(it -> !it.getErOverstyrt())
            .filter(it -> Utfall.IKKE_VURDERT.equals(it.getGjeldendeUtfall()))
            .anyMatch(it -> vilkårsPerioderRaw.stream()
                .map(VilkårPeriode::getPeriode)
                .filter(at -> !at.equals(it.getPeriode()))
                .anyMatch(p -> kantIKantVurderer.erKantIKant(it.getPeriode(), p)));
    }

    private boolean enAvPeriodeneErTilVurdering(VilkårPeriode periode, VilkårPeriode vilkårPeriode) {
        return Utfall.IKKE_VURDERT.equals(periode.getUtfall()) || Utfall.IKKE_VURDERT.equals(vilkårPeriode.getUtfall());
    }
}
