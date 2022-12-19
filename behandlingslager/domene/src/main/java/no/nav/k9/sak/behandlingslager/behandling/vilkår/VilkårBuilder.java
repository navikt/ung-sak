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
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;

public class VilkårBuilder {

    private final Vilkår vilkåret;
    private boolean dummy = false;
    private KantIKantVurderer kantIKantVurderer = new IngenVurdering();
    private LocalDateTimeline<WrappedVilkårPeriode> vilkårTidslinje;
    private LocalDateTimeline<WrappedVilkårPeriode> fagsakTidslinje = null;
    private boolean bygget = false;
    private int mellomliggendePeriodeAvstand = 0;

    /**
     * @deprecated bygger ugyldig vilkår.
     */
    @Deprecated(forRemoval = true)
    public VilkårBuilder() {
        this.vilkåret = new Vilkår();
        this.vilkårTidslinje = new LocalDateTimeline<>(List.of());
    }

    public VilkårBuilder(VilkårType vilkårType) {
        this.vilkåret = new Vilkår(vilkårType);
        this.vilkårTidslinje = new LocalDateTimeline<>(List.of());
    }

    VilkårBuilder(Vilkår vilkåret) {
        this(vilkåret, null);
    }

    VilkårBuilder(Vilkår vilkåret, LocalDateInterval boundry) {
        this.vilkåret = new Vilkår(vilkåret);
        this.vilkårTidslinje = new LocalDateTimeline<>(vilkåret.getPerioder()
            .stream()
            .map(a -> toSegment(a, boundry))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
    }

    private LocalDateSegment<WrappedVilkårPeriode> toSegment(VilkårPeriode a, LocalDateInterval boundry) {
        var vilkårDatoInterval = a.getPeriode().toLocalDateInterval();

        if (boundry != null) {
            var overlapp = vilkårDatoInterval.overlap(boundry);
            if (overlapp.isPresent()) {
                var nyPeriode = overlapp.get();
                var nyVilkårPeriode = new VilkårPeriodeBuilder(a).medPeriode(nyPeriode.getFomDato(), nyPeriode.getTomDato()).build();
                return new LocalDateSegment<>(nyPeriode.getFomDato(), nyPeriode.getTomDato(), new WrappedVilkårPeriode(nyVilkårPeriode));
            } else {
                return null;
            }
        } else {
            return new LocalDateSegment<>(vilkårDatoInterval, new WrappedVilkårPeriode(a));
        }
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

    /**
     * Markerer at denne ikke skal legges til i settet med vilkår
     *
     * @return builder
     */
    public VilkårBuilder somDummy() {
        dummy = true;
        return this;
    }

    /**
     * @deprecated bruk ctor {@link #VilkårBuilder(VilkårType)}.
     */
    @Deprecated(forRemoval = true)
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
        var segment = new LocalDateSegment<>(periode.getPeriode().toLocalDateInterval(), new WrappedVilkårPeriode(periode));

        this.vilkårTidslinje = vilkårTidslinje.combine(segment, this::sjekkVurdering, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return this;
    }
    
    public VilkårBuilder leggTilIkkeVurdert(LocalDate fom, LocalDate tom) {
        validerBuilder();
        final var segment = new LocalDateSegment<>(fom, tom, Boolean.TRUE);
       
        this.vilkårTidslinje = vilkårTidslinje.combine(segment, (p, s1, s2) -> {
            if (s1 == null) {
                final VilkårPeriodeBuilder vpb = hentBuilderFor(p.getFomDato(), p.getTomDato()).medUtfall(Utfall.IKKE_VURDERT);
                return new LocalDateSegment<>(p, new WrappedVilkårPeriode(vpb.build()));
            }
            if (s2 == null) {
                return new LocalDateSegment<>(p, s1.getValue());
            }

            final VilkårPeriode vp = new VilkårPeriodeBuilder(s1.getValue().getVilkårPeriode())
                    .medPeriode(p.getFomDato(), p.getTomDato())
                    .medUtfall(Utfall.IKKE_VURDERT)
                    .build();
            
            return new LocalDateSegment<>(p, new WrappedVilkårPeriode(vp));
        }, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return this;
    }

    public boolean harDataPåPeriode(DatoIntervallEntitet periode) {
        return vilkårTidslinje.intersection(periode.toLocalDateInterval())
            .toSegments()
            .stream()
            .anyMatch(it -> Objects.nonNull(it.getValue()));
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

            this.vilkårTidslinje = vilkårTidslinje.disjoint(periodeTidslinje);
        }
        justereUtfallVedTilbakestilling(perioder);
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

        if (dummy) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke bygge en dummy");
        }
        if (!vilkårTidslinje.isContinuous()) {
            kobleSammenMellomliggendeVilkårsPerioder();
        }
        if (fagsakTidslinje != null) {
            var tidslinjeSomFaltBort = vilkårTidslinje.disjoint(fagsakTidslinje);
            vilkårTidslinje = vilkårTidslinje.intersection(fagsakTidslinje);
            var periodeneSomFaltBort = TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeSomFaltBort.filterValue(Objects::nonNull));
            justereUtfallVedTilbakestilling(periodeneSomFaltBort);
        }
        bygget = true;
        if (kantIKantVurderer.erKomprimerbar()) {
            vilkårTidslinje = vilkårTidslinje.compress();
        }
        var vilkårsPerioderRaw = vilkårTidslinje
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(this::opprettHoldKonsistens)
            .map(WrappedVilkårPeriode::getVilkårPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
        var vilkårsPerioder = sammenkobleOgJusterUtfallHvisEnPeriodeTilVurdering(vilkårsPerioderRaw);
        vilkåret.setPerioder(new ArrayList<>(vilkårsPerioder));
        Objects.requireNonNull(vilkåret.getVilkårType(), "Mangler vilkårType");
        return vilkåret;
    }

    VilkårBuilder medFullstendigTidslinje(LocalDateTimeline<WrappedVilkårPeriode> fagsakTidslinje) {
        this.fagsakTidslinje = fagsakTidslinje;
        return this;
    }

    private void justereUtfallVedTilbakestilling(NavigableSet<DatoIntervallEntitet> tilbakestiltePerioder) {
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
            if (harDataPåPeriode(datoIntervallEntitet)) {
                var periodeBuilder = hentBuilderFor(datoIntervallEntitet)
                    .medUtfall(Utfall.IKKE_VURDERT);
                leggTil(periodeBuilder);
            }
        }
    }

    private NavigableSet<VilkårPeriode> sammenkobleOgJusterUtfallHvisEnPeriodeTilVurdering(NavigableSet<VilkårPeriode> vilkårsPerioderRaw) {
        var periodeTilVurdering = vilkårsPerioderRaw.stream().anyMatch(it -> Utfall.IKKE_VURDERT.equals(it.getUtfall()));
        var perioderSomGrenserTil = harPerioderSomIkkeErVurdertOgGrenserTilAnnenPeriode(vilkårsPerioderRaw);

        if (perioderSomGrenserTil && periodeTilVurdering) {
            VilkårPeriode periode = null;
            var vilkårPerioder = new ArrayList<VilkårPeriode>();

            for (VilkårPeriode vilkårPeriode : vilkårsPerioderRaw) {
                if (periode == null) {
                    periode = vilkårPeriode;
                } else if (kantIKantVurderer.erKantIKant(vilkårPeriode.getPeriode(), periode.getPeriode())
                        && enAvPeriodeneErTilVurdering(periode, vilkårPeriode)) {
                    if (vilkåret.getVilkårType() == null
                            || !vilkåret.getVilkårType().isKanOverstyresPeriodisert()
                            || harSammeOverstyring(periode, vilkårPeriode)) {
                        /*
                         * OBS: Denne logikken forlenger en eventuell overstyring som ligger i periode
                         * til å gjelde for vilkårPeriode også.
                         */
                        periode = new VilkårPeriodeBuilder(periode)
                            .medPeriode(periode.getFom(), vilkårPeriode.getTom())
                            .medUtfall(Utfall.IKKE_VURDERT)
                            .build();
                    } else {
                        /*
                         * Vi kan ikke slå sammen perioder med forskjellige overstyringer. Derfor blir
                         * disse periodene lagt in separat.
                         */
                        if (!periode.getTom().plusDays(1).equals(vilkårPeriode.getFom())) {
                            /*
                             * Forlenger en eventuell overstyring til etterfølgende helg.
                             */
                            vilkårPerioder.add(new VilkårPeriodeBuilder(periode)
                                    .medPeriode(periode.getFom(), vilkårPeriode.getFom().minusDays(1))
                                    .medUtfall(Utfall.IKKE_VURDERT)
                                    .build());
                        } else {
                            vilkårPerioder.add(new VilkårPeriodeBuilder(periode)
                                    .medUtfall(Utfall.IKKE_VURDERT)
                                    .build());
                        }
                        
                        periode = new VilkårPeriodeBuilder(vilkårPeriode)
                                .medUtfall(Utfall.IKKE_VURDERT)
                                .build();
                    }
                } else {
                    vilkårPerioder.add(periode);
                    periode = vilkårPeriode;
                }
            }
            if (periode != null) {
                vilkårPerioder.add(periode);
            }
            return adjustAndCompress(vilkårPerioder);
        }
        
        return vilkårsPerioderRaw;
    }
    
    private boolean harSammeOverstyring(VilkårPeriode vp1, VilkårPeriode vp2) {
        if (!vp1.getErOverstyrt() && !vp2.getErOverstyrt()) {
            return true;
        }
        return vp1.getOverstyrtUtfall() == vp2.getOverstyrtUtfall()
                && vp1.getBegrunnelse().equals(vp2.getBegrunnelse());
    }

    private NavigableSet<VilkårPeriode> adjustAndCompress(List<VilkårPeriode> vilkårPerioder) {
        var timeline = new LocalDateTimeline<>(vilkårPerioder.stream()
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new WrappedVilkårPeriode(it)))
            .collect(Collectors.toList()));
        if (kantIKantVurderer.erKomprimerbar()) {
            timeline = timeline.compress();
        }
        return timeline.toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(this::opprettHoldKonsistens)
            .map(WrappedVilkårPeriode::getVilkårPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean harPerioderSomIkkeErVurdertOgGrenserTilAnnenPeriode(NavigableSet<VilkårPeriode> vilkårsPerioderRaw) {
        return vilkårsPerioderRaw.stream()
            .filter(it -> Utfall.IKKE_VURDERT.equals(it.getUtfall()))
            .anyMatch(it -> vilkårsPerioderRaw.stream()
                .map(VilkårPeriode::getPeriode)
                .filter(at -> !at.equals(it.getPeriode()))
                .anyMatch(p -> kantIKantVurderer.erKantIKant(it.getPeriode(), p)));
    }

    private boolean enAvPeriodeneErTilVurdering(VilkårPeriode periode, VilkårPeriode vilkårPeriode) {
        return Utfall.IKKE_VURDERT.equals(periode.getUtfall()) || Utfall.IKKE_VURDERT.equals(vilkårPeriode.getUtfall());
    }

    LocalDateTimeline<WrappedVilkårPeriode> getTidslinje() {
        if (!dummy) {
            throw new IllegalStateException("Ikke dummy så kan ikke hente ut ");
        }
        bygget = true;
        if (!vilkårTidslinje.isContinuous()) {
            kobleSammenMellomliggendeVilkårsPerioder();
        }

        var vilkårsPerioderRaw = vilkårTidslinje
            .toSegments()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(this::opprettHoldKonsistens)
            .map(WrappedVilkårPeriode::getVilkårPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
        var vilkårsPerioder = sammenkobleOgJusterUtfallHvisEnPeriodeTilVurdering(vilkårsPerioderRaw)
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new WrappedVilkårPeriode(it)))
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(vilkårsPerioder);
    }
}
