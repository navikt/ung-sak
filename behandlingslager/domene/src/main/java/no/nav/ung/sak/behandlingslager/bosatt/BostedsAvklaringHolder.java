package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregat/holder for bostedsavklaringer. Kan deles mellom behandlinger
 * ved revurdering uten endringer i grunnlaget.
 * Inneholder ett {@link BostedsPeriodeAvklaring} per vilkårsperiode.
 */
@Entity(name = "BostedsAvklaringHolder")
@Table(name = "BOSATT_AVKLARING_HOLDER")
public class BostedsAvklaringHolder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_AVKLARING_HOLDER")
    private Long id;

    @BatchSize(size = 20)
    @JoinColumn(name = "bosatt_avklaring_holder_id", nullable = false)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BostedsPeriodeAvklaring> periodeAvklaringer = new LinkedHashSet<>();

    public BostedsAvklaringHolder() {
    }

    BostedsAvklaringHolder(BostedsAvklaringHolder other) {
        if (other != null && other.periodeAvklaringer != null) {
            this.periodeAvklaringer = other.periodeAvklaringer.stream()
                .map(p -> new BostedsPeriodeAvklaring(p.getPeriode(), p.isErBosattITrondheim(), p.getFraflyttingsÅrsak(), p.getKilde()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    BostedsAvklaringHolder(Set<BostedsPeriodeAvklaring> periodeAvklaringer) {
        this.periodeAvklaringer = periodeAvklaringer;
    }

    void fjernPeriodeAvklaringForFom(LocalDate fom) {
        periodeAvklaringer.removeIf(it -> it.getPeriode().getFomDato().equals(fom));
    }

    void leggTilPeriodeAvklaring(BostedsPeriodeAvklaring periodeAvklaring) {
        periodeAvklaringer.add(periodeAvklaring);
    }

    void leggTilPeriodeAvklaringer(Collection<BostedsPeriodeAvklaring> periodeAvklaring) {
        periodeAvklaringer.addAll(periodeAvklaring);
    }

    public Long getId() {
        return id;
    }

    public Set<BostedsPeriodeAvklaring> getPeriodeAvklaringer() {
        return Collections.unmodifiableSet(periodeAvklaringer);
    }

    public Optional<BostedsPeriodeAvklaring> getPeriodeAvklaring(LocalDate fom) {
        return periodeAvklaringer.stream().filter(it -> it.getPeriode().getFomDato().equals(fom)).findFirst();
    }


    public Optional<BostedsPeriodeAvklaring> getPeriodeAvklaring(UUID ref) {
        return periodeAvklaringer.stream().filter(it -> it.getReferanse().equals(ref)).findFirst();
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
