package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregat for bostedsavklaring knyttet til én vilkårsperiode.
 * {@code skjæringstidspunkt} tilsvarer fom-dato for vilkårsperioden og matcher
 * fom-dato til tilhørende Etterlysning og UttalelseV2.
 * {@code referanse} brukes som {@code grunnlagsreferanse} i etterlysning og uttalelse.
 * {@code erBosattITrondheim} angir om bruker er bosatt ved skjæringstidspunktet.
 * {@code fraflyttingsDato} angir eventuell dato for utflytting fra Trondheim (null dersom bruker ikke har flyttet ut).
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

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

    @Column(name = "fraflyttings_dato", updatable = false)
    private LocalDate fraflyttingsDato;

    public BostedsPeriodeAvklaring() {
        // Hibernate
    }

    public BostedsPeriodeAvklaring(LocalDate skjæringstidspunkt, boolean erBosattITrondheim, LocalDate fraflyttingsDato) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.erBosattITrondheim = erBosattITrondheim;
        this.fraflyttingsDato = fraflyttingsDato;
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

    public boolean isErBosattITrondheim() {
        return erBosattITrondheim;
    }

    public LocalDate getFraflyttingsDato() {
        return fraflyttingsDato;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsPeriodeAvklaring that)) return false;
        return erBosattITrondheim == that.erBosattITrondheim
            && Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt)
            && Objects.equals(fraflyttingsDato, that.fraflyttingsDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt, erBosattITrondheim, fraflyttingsDato);
    }

    @Override
    public String toString() {
        return "BostedsPeriodeAvklaring{skjæringstidspunkt=" + skjæringstidspunkt
            + ", referanse=" + referanse
            + ", erBosattITrondheim=" + erBosattITrondheim
            + ", fraflyttingsDato=" + fraflyttingsDato + '}';
    }
}
