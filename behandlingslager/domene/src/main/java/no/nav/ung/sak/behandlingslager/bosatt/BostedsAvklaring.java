package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Fakta-avklaring om brukers bosted for én periode.
 * {@code fomDato} er startdatoen for avklaringsperioden (vilkårsperiodens fom, eller fraflyttingsdato ved delt periode).
 */
@Entity(name = "BostedsAvklaring")
@Table(name = "BOSATT_AVKLARING")
@Immutable
public class BostedsAvklaring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_AVKLARING")
    private Long id;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fomDato;

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

    public BostedsAvklaring() {
        // Hibernate
    }

    public BostedsAvklaring(LocalDate fomDato, boolean erBosattITrondheim) {
        this.fomDato = fomDato;
        this.erBosattITrondheim = erBosattITrondheim;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public boolean erBosattITrondheim() {
        return erBosattITrondheim;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsAvklaring that)) return false;
        return erBosattITrondheim == that.erBosattITrondheim
            && Objects.equals(fomDato, that.fomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fomDato, erBosattITrondheim);
    }

    @Override
    public String toString() {
        return "BostedsAvklaring{fomDato=" + fomDato
            + ", erBosattITrondheim=" + erBosattITrondheim + '}';
    }
}
