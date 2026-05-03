package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
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
        this.periodeAvklaringer = other.periodeAvklaringer.stream()
            .map(p -> new BostedsPeriodeAvklaring(p.getSkjæringstidspunkt(), p.isErBosattITrondheim(), p.getFraflyttingsDato(), p.getFraflyttingsÅrsak()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    void leggTilPeriodeAvklaring(BostedsPeriodeAvklaring periodeAvklaring) {
        periodeAvklaringer.removeIf(p -> p.getSkjæringstidspunkt().equals(periodeAvklaring.getSkjæringstidspunkt()));
        periodeAvklaringer.add(periodeAvklaring);
    }

    public Long getId() {
        return id;
    }

    public Set<BostedsPeriodeAvklaring> getPeriodeAvklaringer() {
        return Collections.unmodifiableSet(periodeAvklaringer);
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
