package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregat for bostedsavklaringer knyttet til én vilkårsperiode.
 * {@code skjæringstidspunkt} tilsvarer fom-dato for vilkårsperioden og matcher
 * fom-dato til tilhørende {@link Etterlysning} og {@link UttalelseV2}.
 * {@code referanse} brukes som {@code grunnlagsreferanse} i etterlysning og uttalelse.
 */
@Entity(name = "BostedsPeriodeAvklaring")
@Table(name = "BOSATT_PERIODE_AVKLARING")
public class BostedsPeriodeAvklaring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_PERIODE_AVKLARING")
    private Long id;

    @Column(name = "referanse", nullable = false, updatable = false)
    private UUID referanse = UUID.randomUUID();

    @Column(name = "skaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @BatchSize(size = 20)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bosatt_periode_avklaring_id", nullable = false)
    private Set<BostedsAvklaring> avklaringer = new LinkedHashSet<>();

    public BostedsPeriodeAvklaring() {
        // Hibernate
    }

    public BostedsPeriodeAvklaring(LocalDate skjæringstidspunkt, Set<BostedsAvklaring> avklaringer) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.avklaringer.addAll(avklaringer);
    }

    public Long getId() {
        return id;
    }

    public UUID getReferanse() {
        return referanse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public Set<BostedsAvklaring> getAvklaringer() {
        return Collections.unmodifiableSet(avklaringer);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsPeriodeAvklaring that)) return false;
        return Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt)
            && Objects.equals(avklaringer, that.avklaringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt, avklaringer);
    }

    @Override
    public String toString() {
        return "BostedsPeriodeAvklaring{skjæringstidspunkt=" + skjæringstidspunkt
            + ", referanse=" + referanse
            + ", avklaringer=" + avklaringer + '}';
    }
}
