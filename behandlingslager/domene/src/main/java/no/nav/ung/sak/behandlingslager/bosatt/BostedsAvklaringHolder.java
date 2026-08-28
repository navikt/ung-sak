package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregat/holder for bostedsavklaringer. Holderen kan deles i sin helhet mellom behandlinger ved revurdering uten endringer i grunnlaget.
 * Enhver endring fører til en ny instans av holderen med kopier. Dette er ivaretatt av setter på grunnlag!
 * <p>
 * Holderen har to samlinger:
 * <ul>
 *     <li>{@code periodeAvklaringerForeslått} — avklaringer som er foreslått og behandlet i gjeldende behandling.
 *     Disse kopieres aldri videre til en ny behandling.</li>
 *     <li>{@code periodeAvklaringerFerdigstilt} — alle ferdigstilte (vedtatte) avklaringer. Disse akkumuleres og følger saken videre.</li>
 * </ul>
 * Hvilken samling en avklaring ligger i erstatter behovet for status og opprettende behandling på selve avklaringen.
 * <p>
 * Klassen er pakkeprivat med vilje: mutasjon skal kun skje gjennom setterne på {@link BostedsGrunnlag}, som sørger for
 * at det lages en ny holder-instans ved endring slik at data fra tidligere behandlinger aldri muteres.
 */
@Entity(name = "BostedsAvklaringHolder")
@Table(name = "BOSATT_AVKLARING_HOLDER")
class BostedsAvklaringHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_AVKLARING_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosatt_avklaring_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsPeriodeAvklaringForeslått> periodeAvklaringerForeslått = new LinkedHashSet<>();

    @BatchSize(size = 20)
    @JoinColumn(name = "bosatt_avklaring_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsPeriodeAvklaringFerdigstilt> periodeAvklaringerFerdigstilt = new LinkedHashSet<>();

    public BostedsAvklaringHolder() {
    }

    private BostedsAvklaringHolder(BostedsAvklaringHolder other, boolean inkluderForeslåtte) {
        if (other == null) {
            return;
        }
        this.periodeAvklaringerFerdigstilt = other.periodeAvklaringerFerdigstilt.stream()
            .map(BostedsPeriodeAvklaringFerdigstilt::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (inkluderForeslåtte) {
            this.periodeAvklaringerForeslått = other.periodeAvklaringerForeslått.stream()
                .map(BostedsPeriodeAvklaringForeslått::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    /**
     * Lager en kopi av holderen med begge samlingene. Brukes av setterne på {@link BostedsGrunnlag} for å unngå å
     * mutere en holder som kan være delt med tidligere behandlinger.
     */
    static BostedsAvklaringHolder lagKopi(BostedsAvklaringHolder other) {
        return new BostedsAvklaringHolder(other, true);
    }

    /**
     * Lager en kopi uten de foreslåtte avklaringene. Brukes når grunnlaget kopieres til en ny behandling —
     * forslag som ble gjort i en tidligere behandling skal ikke gjelde for den nye behandlingen.
     */
    static BostedsAvklaringHolder lagKopiUtenForeslåtte(BostedsAvklaringHolder other) {
        return new BostedsAvklaringHolder(other, false);
    }

    /**
     * Erstatter de foreslåtte avklaringene istedenfor å legge til eller splitte eksisterende — slik at referansen til én
     * foreslått periodeavklaring alltid dekker ett segment og kan varsles entydig. Ferdigstilte avklaringer beholdes urørt.
     * Obs: Denne muterer og må kun kalles gjennom setter på grunnlag, slik at deduplisering gjøres korrekt.
     */
    void erstattForeslåttePeriodeAvklaringer(Collection<BostedsPeriodeAvklaringForeslått> nyePeriodeAvklaringer) {
        periodeAvklaringerForeslått = nyePeriodeAvklaringer.stream()
            .map(BostedsPeriodeAvklaringForeslått::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Ferdigstiller de foreslåtte avklaringene ved å kopiere dem over blant de ferdigstilte, der de overstyrer
     * tidligere ferdigstilte avklaringer i overlappende perioder. Tidligere ferdigstilte avklaringer kan dermed
     * splittes og dele referanse på tvers av segmenter — det etterlyses aldri uttalelse på en ferdigstilt avklaring.
     * <p>
     * De foreslåtte avklaringene beholdes, siden de forteller hva som ble foreslått og behandlet i denne behandlingen.
     * Operasjonen er derfor idempotent.
     * Obs: Denne muterer periodeAvklaringerFerdigstilt og må kun kalles gjennom setter på grunnlag, slik at deduplisering gjøres korrekt.
     */
    void ferdigstillForeslåtteAvklaringer() {
        // Konverterer de foreslåtte til ferdigstilte før tidslinjeoperasjonene, slik at begge tidslinjene har samme
        // type og eksisterende ferdigstilte instanser kun berøres når perioden deres faktisk splittes.
        var foreslåtteTidslinje = byggAvklaringTidslinje(periodeAvklaringerForeslått.stream()
            .map(BostedsPeriodeAvklaringFerdigstilt::new)
            .toList());

        periodeAvklaringerFerdigstilt = byggAvklaringTidslinje(periodeAvklaringerFerdigstilt)
            .disjoint(foreslåtteTidslinje)
            .crossJoin(foreslåtteTidslinje)
            .stream()
            .map(segment -> medPeriode(segment.getValue(), DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom())))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Beholder instansen som den er hvis perioden er uendret, slik at kun de faktisk splittede avklaringene erstattes av nye instanser.
    private static BostedsPeriodeAvklaringFerdigstilt medPeriode(BostedsPeriodeAvklaringFerdigstilt avklaring, DatoIntervallEntitet periode) {
        return avklaring.getPeriode().equals(periode) ? avklaring : new BostedsPeriodeAvklaringFerdigstilt(avklaring, periode);
    }

    Long getId() {
        return id;
    }

    /**
     * Alle ferdigstilte (vedtatte) avklaringer, akkumulert på tvers av behandlinger.
     */
    Set<BostedsPeriodeAvklaring> hentFerdigstilteAvklaringer() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(periodeAvklaringerFerdigstilt));
    }

    /**
     * Avklaringer som er foreslått og behandlet i den behandlingen grunnlaget tilhører.
     */
    Set<BostedsPeriodeAvklaring> hentForeslåtteAvklaringer() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(periodeAvklaringerForeslått));
    }

    static <V extends BostedsPeriodeAvklaring> LocalDateTimeline<V> byggAvklaringTidslinje(Collection<? extends V> avklaringer) {
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
        if (!(o instanceof BostedsAvklaringHolder that)) return false;
        return Objects.equals(periodeAvklaringerFerdigstilt, that.periodeAvklaringerFerdigstilt)
            && Objects.equals(periodeAvklaringerForeslått, that.periodeAvklaringerForeslått);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periodeAvklaringerFerdigstilt, periodeAvklaringerForeslått);
    }
}
