package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Fakta-avklaring om brukers bosted for ett skjæringstidspunkt.
 * Skjæringstidspunktet er fom-datoen i den tilhørende vilkårsperioden.
 */
@Entity(name = "BostedsAvklaring")
@Table(name = "BOSATT_AVKLARING")
@Immutable
public class BostedsAvklaring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_AVKLARING")
    private Long id;

    @Column(name = "bosatt_avklaring_holder_id", nullable = false, updatable = false)
    private Long holderId;

    @Column(name = "skjaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

    public BostedsAvklaring() {
        // Hibernate
    }

    public BostedsAvklaring(LocalDate skjæringstidspunkt, boolean erBosattITrondheim) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.erBosattITrondheim = erBosattITrondheim;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public boolean erBosattITrondheim() {
        return erBosattITrondheim;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsAvklaring that)) return false;
        return erBosattITrondheim == that.erBosattITrondheim
            && Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt, erBosattITrondheim);
    }

    @Override
    public String toString() {
        return "BostedsAvklaring{skjæringstidspunkt=" + skjæringstidspunkt
            + ", erBosattITrondheim=" + erBosattITrondheim + '}';
    }
}
