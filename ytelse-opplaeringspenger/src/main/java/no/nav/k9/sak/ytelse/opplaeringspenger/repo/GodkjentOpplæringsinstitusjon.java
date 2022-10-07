package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "GodkjentOpplæringsinstitusjon")
@Table(name = "GODKJENTE_OPPLAERINGSINSTITUSJONER")
public class GodkjentOpplæringsinstitusjon extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GODKJENTE_OPPLAERINGSINSTITUSJONER")
    private Long id;

    @NaturalId
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "navn", nullable = false)
    private String navn;

    @Column(name = "fom")
    private LocalDate fomDato;

    @Column(name = "tom")
    private LocalDate tomDato;

    public GodkjentOpplæringsinstitusjon() {
    }

    public GodkjentOpplæringsinstitusjon(UUID uuid, String navn, LocalDate fomDato, LocalDate tomDato) {
        this.uuid = uuid;
        this.navn = navn;
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GodkjentOpplæringsinstitusjon that = (GodkjentOpplæringsinstitusjon) o;
        return Objects.equals(uuid, that.uuid)
            && Objects.equals(navn, that.navn)
            && Objects.equals(fomDato, that.fomDato)
            && Objects.equals(tomDato, that.tomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, navn, fomDato, tomDato);
    }

    @Override
    public String toString() {
        return "GodkjentInstitusjon{" +
            "uuid=" + uuid +
            ", navn=" + navn +
            ", fomDato=" + fomDato +
            ", tomDato=" + tomDato +
            '}';
    }
}
