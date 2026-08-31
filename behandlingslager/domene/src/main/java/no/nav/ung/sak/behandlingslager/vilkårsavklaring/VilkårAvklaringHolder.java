package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregat/holder for ferdigstilte vilkårsavklaringer for ett vilkår. Holderen kan deles i sin helhet mellom
 * behandlinger ved revurdering uten endringer. Enhver endring fører til en ny instans av holderen med kopier.
 * Dette er ivaretatt av setterne på {@link VilkårsavklaringGrunnlag}.
 * <p>
 * I motsetning til bosted-modellen inneholder holderen kun ferdigstilte avklaringer. Foreslåtte avklaringer
 * gjelder per definisjon kun for én behandling og deles aldri, og hører derfor hjemme på grunnlaget —
 * se {@link VilkårsavklaringGrunnlag}.
 * <p>
 * Klassen er pakkeprivat med vilje: mutasjon skal kun skje gjennom setterne på {@link VilkårsavklaringGrunnlag},
 * som sørger for at det lages en ny holder-instans ved endring slik at data fra tidligere behandlinger aldri
 * muteres.
 */
@Entity(name = "VilkårAvklaringHolder")
@Table(name = "VILKAAR_AVKLARING_HOLDER")
class VilkårAvklaringHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAAR_AVKLARING_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "vilkaar_avklaring_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VilkårPeriodeAvklaringFerdigstilt> periodeAvklaringerFerdigstilt = new LinkedHashSet<>();

    public VilkårAvklaringHolder() {
    }

    private VilkårAvklaringHolder(VilkårAvklaringHolder other) {
        if (other == null) {
            return;
        }
        this.periodeAvklaringerFerdigstilt = other.periodeAvklaringerFerdigstilt.stream()
            .map(VilkårPeriodeAvklaringFerdigstilt::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Lager en kopi av holderen. Brukes av setterne på {@link VilkårsavklaringGrunnlag} for å unngå å mutere en
     * holder som kan være delt med tidligere behandlinger.
     */
    static VilkårAvklaringHolder lagKopi(VilkårAvklaringHolder other) {
        return new VilkårAvklaringHolder(other);
    }

    /**
     * Ferdigstiller de foreslåtte avklaringene ved å kopiere dem over blant de ferdigstilte, der de overstyrer
     * tidligere ferdigstilte avklaringer i overlappende perioder. Tidligere ferdigstilte avklaringer kan dermed
     * splittes og dele referanse på tvers av segmenter — det etterlyses aldri uttalelse på en ferdigstilt avklaring.
     * Obs: Denne muterer periodeAvklaringerFerdigstilt og må kun kalles gjennom setter på grunnlag, slik at
     * deduplisering gjøres korrekt.
     */
    void ferdigstillAvklaringer(Collection<VilkårPeriodeAvklaringForeslått> foreslåtteAvklaringer) {
        // Konverterer de foreslåtte til ferdigstilte før tidslinjeoperasjonene, slik at begge tidslinjene har samme
        // type og eksisterende ferdigstilte instanser kun berøres når perioden deres faktisk splittes.
        var foreslåtteTidslinje = byggAvklaringTidslinje(foreslåtteAvklaringer.stream()
            .map(VilkårPeriodeAvklaringFerdigstilt::new)
            .toList());

        periodeAvklaringerFerdigstilt = byggAvklaringTidslinje(periodeAvklaringerFerdigstilt)
            .disjoint(foreslåtteTidslinje)
            .crossJoin(foreslåtteTidslinje)
            .stream()
            .map(segment -> medPeriode(segment.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom())))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Beholder instansen som den er hvis perioden er uendret, slik at kun de faktisk splittede avklaringene erstattes av nye instanser.
    private static VilkårPeriodeAvklaringFerdigstilt medPeriode(VilkårPeriodeAvklaringFerdigstilt avklaring, DatoIntervallEntitet periode) {
        return avklaring.getPeriode().equals(periode) ? avklaring : new VilkårPeriodeAvklaringFerdigstilt(avklaring, periode);
    }

    Long getId() {
        return id;
    }

    /**
     * Alle ferdigstilte (vedtatte) avklaringer, akkumulert på tvers av behandlinger.
     */
    Set<VilkårPeriodeAvklaring> hentFerdigstilteAvklaringer() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(periodeAvklaringerFerdigstilt));
    }

    static <V extends VilkårPeriodeAvklaring> LocalDateTimeline<V> byggAvklaringTidslinje(Collection<? extends V> avklaringer) {
        return new LocalDateTimeline<>(
            avklaringer.stream().map(avklaring ->
                new LocalDateSegment<V>(
                    avklaring.getPeriode().getFomDato(),
                    avklaring.getPeriode().getTomDato(),
                    avklaring)
            ).collect(Collectors.toList())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VilkårAvklaringHolder that)) return false;
        return Objects.equals(periodeAvklaringerFerdigstilt, that.periodeAvklaringerFerdigstilt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodeAvklaringerFerdigstilt);
    }
}
