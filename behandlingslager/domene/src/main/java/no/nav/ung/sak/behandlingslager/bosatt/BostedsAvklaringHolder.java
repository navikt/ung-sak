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
    private Set<BostedsAvklaring> avklaringer = new LinkedHashSet<>();

    public BostedsAvklaringHolder() {
    }

    BostedsAvklaringHolder(BostedsAvklaringHolder other) {
        this.avklaringer = other.avklaringer.stream()
            .map(a -> new BostedsAvklaring(a.getSkjæringstidspunkt(), a.erBosattITrondheim()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    void leggTilAvklaring(BostedsAvklaring avklaring) {
        avklaringer.removeIf(a -> a.getSkjæringstidspunkt().equals(avklaring.getSkjæringstidspunkt()));
        avklaringer.add(avklaring);
    }

    public Long getId() {
        return id;
    }

    public Set<BostedsAvklaring> getAvklaringer() {
        return Collections.unmodifiableSet(avklaringer);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsAvklaringHolder that)) return false;
        return Objects.equals(avklaringer, that.avklaringer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(avklaringer);
    }
}
