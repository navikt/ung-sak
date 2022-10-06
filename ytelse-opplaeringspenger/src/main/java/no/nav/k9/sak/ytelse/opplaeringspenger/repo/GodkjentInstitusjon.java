package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "GodkjentInstitusjon")
@Table(name = "olp_godkjent_institusjon")
public class GodkjentInstitusjon extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_GODKJENT_INSTITUSJON")
    private Long id;

    @Column(name = "navn", nullable = false)
    private String navn;

    @Column(name = "fom")
    private LocalDate fomDato;

    @Column(name = "tom")
    private LocalDate tomDato;

    public GodkjentInstitusjon() {
    }

    public GodkjentInstitusjon(String navn, LocalDate fomDato, LocalDate tomDato) {
        this.navn = navn;
        this.fomDato = fomDato;
        this.tomDato = tomDato;
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
        GodkjentInstitusjon that = (GodkjentInstitusjon) o;
        return Objects.equals(navn, that.navn)
            && Objects.equals(fomDato, that.fomDato)
            && Objects.equals(tomDato, that.tomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(navn, fomDato, tomDato);
    }

    @Override
    public String toString() {
        return "GodkjentInstitusjon{" +
            "navn=" + navn +
            ", fomDato=" + fomDato +
            ", tomDato=" + tomDato +
            '}';
    }
}
