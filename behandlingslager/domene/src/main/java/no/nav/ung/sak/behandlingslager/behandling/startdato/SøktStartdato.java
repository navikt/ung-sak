package no.nav.ung.sak.behandlingslager.behandling.startdato;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.diff.ChangeTracked;
import no.nav.ung.sak.typer.JournalpostId;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.Objects;

import static no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode.SøktPeriodeData;

@Entity(name = "SøktStartdato")
@Table(name = "SOEKT_STARTDATO")
@Immutable
public class SøktStartdato extends BaseEntitet implements SøktPeriodeData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOEKT_STARTDATO")
    private Long id;


    @ChangeTracked
    @Column(name = "startdato", nullable = false)
    private LocalDate startdato;

    @ChangeTracked
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public SøktStartdato(LocalDate startdato, JournalpostId journalpostId) {
        this.startdato = startdato;
        this.journalpostId = journalpostId;
    }

    public SøktStartdato(SøktStartdato it) {
        this.journalpostId = it.getJournalpostId();
        this.startdato = it.getStartdato();
    }

    public SøktStartdato() {
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
        if (o == null || getClass() != o.getClass()) return false;
        SøktStartdato that = (SøktStartdato) o;
        return Objects.equals(startdato, that.startdato)
            && Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startdato, journalpostId);
    }

    @Override
    public String toString() {
        return "SøktStartdato{" +
            "id=" + id +
            ", startdato=" + startdato +
            ", journalpostId=" + journalpostId +
            ", versjon=" + versjon +
            '}';
    }
}
