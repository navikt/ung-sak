package no.nav.ung.sak.behandlingslager.behandling.startdato;

import static no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode.*;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.JournalpostId;

@Entity(name = "UngdomsytelseSøktStartdato")
@Table(name = "UNG_SOEKT_STARTDATO")
@Immutable
public class UngdomsytelseSøktStartdato extends BaseEntitet implements SøktPeriodeData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKT_STARTDATO")
    private Long id;


    @Column(name = "startdato", nullable = false)
    private LocalDate startdato;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøktStartdato(LocalDate startdato, JournalpostId journalpostId) {
        this.startdato = startdato;
        this.journalpostId = journalpostId;
    }

    public UngdomsytelseSøktStartdato(UngdomsytelseSøktStartdato it) {
        this.journalpostId = it.getJournalpostId();
        this.startdato = it.getStartdato();
    }

    public UngdomsytelseSøktStartdato() {
        // hibernate
    }

    public LocalDate getStartdato() {
        return startdato;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    @Override
    public <V> V getPayload() {
        // skal returnere data til bruk ved komprimering av perioder (dvs. uten periode)
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøktStartdato that = (UngdomsytelseSøktStartdato) o;
        return Objects.equals(startdato, that.startdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startdato);
    }

    @Override
    public String toString() {
        return "UngdomsytelseSøktStartdato{" +
            "id=" + id +
            ", startdato=" + startdato +
            ", journalpostId=" + journalpostId +
            ", versjon=" + versjon +
            '}';
    }
}
