package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.AvklaringStatus;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregat/holder for bostedsavklaringer. Holderen kan deles i sin helhet mellom behandlinger ved revurdering uten endringer i grunnlaget.
 * Enhver endring fører til en ny instans av holderen med kopier. Dette er ivaretatt av setter på grunnlag!
 * Inneholder ett {@link BostedsPeriodeAvklaring} per vilkårsperiode.
 */
@Entity(name = "BostedsAvklaringHolder")
@Table(name = "BOSATT_AVKLARING_HOLDER")
class BostedsAvklaringHolder extends BaseEntitet implements BostedsAvklaringHolderSkrivebeskyttet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_AVKLARING_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosatt_avklaring_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsPeriodeAvklaring> periodeAvklaringer = new LinkedHashSet<>();

    public BostedsAvklaringHolder() {
    }

    private BostedsAvklaringHolder(BostedsAvklaringHolder other) {
        if (other != null && other.periodeAvklaringer != null) {
            this.periodeAvklaringer = other.periodeAvklaringer.stream()
                .map(BostedsPeriodeAvklaring::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    static BostedsAvklaringHolder lagSkrivbarKopi(BostedsAvklaringHolder other) {
        return new BostedsAvklaringHolder(other);
    }

    /**
     * Erstatter avklaringer under arbeid istedenfor å legge til eller splitte eksisterende — slik at referansen til én periodeavklaring under arbeid alltid dekker
     * ett segment og kan varsles entydig. Avklaringer som allerede er ferdigstilte beholdes urørt.
     * Obs: Denne muterer og må kun kalles gjennom setter på grunnlag, slik at deduplisering gjøres korrekt.
     */
    void leggTilEllerErstattPeriodeAvklaringerUnderArbeid(Collection<BostedsPeriodeAvklaring> nyePeriodeAvklaring) {
        var beholdte = periodeAvklaringer.stream()
            .filter(eksisterende -> eksisterende.getStatus() == AvklaringStatus.FERDIG)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        beholdte.addAll(nyePeriodeAvklaring);
        periodeAvklaringer = beholdte;
    }

    /**
     * Ferdigstiller alle avklaringer under arbeid. Ferdigstilte avklaringer kan splittes av senere
     * ferdigstillinger og kan derfor dele referanse på tvers av segmenter — det etterlyses aldri
     * uttalelse på en ferdigstilt avklaring.
     * Obs: Denne muterer og må kun kalles gjennom setter på grunnlag, slik at deduplisering gjøres korrekt.
     */
    void settAlleAvklaringerTilFerdig() {
        var avklaringerEndretTilFerdig = hentPeriodeAvklaringerMedStatus(AvklaringStatus.UNDER_ARBEID).stream()
            .map(BostedsPeriodeAvklaring::medStatusFerdig)
            .toList();

        var alleFerdige = byggAvklaringTidslinje(avklaringerEndretTilFerdig)
            .crossJoin(hentAvklaringMedStatusSomTidslinje(AvklaringStatus.FERDIG))
            .segmenter().stream()
            .map(s -> s.getValue().medNyPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom())))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        periodeAvklaringer.clear();
        periodeAvklaringer.addAll(alleFerdige);
    }

    private LocalDateTimeline<BostedsPeriodeAvklaring> hentAvklaringMedStatusSomTidslinje(AvklaringStatus... status) {
        return byggAvklaringTidslinje(
            hentPeriodeAvklaringerMedStatus(status)
        );
    }

    @Override
    public List<BostedsPeriodeAvklaring> hentPeriodeAvklaringerMedStatus(AvklaringStatus... status) {
        var statusSet = Set.of(status);
        return hentPeriodeAvklaringer().stream().filter(it -> statusSet.contains(it.getStatus())).toList();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Set<BostedsPeriodeAvklaring> hentPeriodeAvklaringer() {
        return Collections.unmodifiableSet(periodeAvklaringer);
    }

    static LocalDateTimeline<BostedsPeriodeAvklaring> byggAvklaringTidslinje(Collection<BostedsPeriodeAvklaring> avklaringer) {
        return new LocalDateTimeline<>(
            avklaringer.stream().map(avklaring ->
                new LocalDateSegment<>(
                    avklaring.getPeriode().getFomDato(),
                    avklaring.getPeriode().getTomDato(),
                    avklaring)
            ).collect(Collectors.toList())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsAvklaringHolder that)) return false;
        return Objects.equals(periodeAvklaringer, that.periodeAvklaringer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(periodeAvklaringer);
    }
}
