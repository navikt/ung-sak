package no.nav.ung.sak.behandlingslager.behandling.startdato;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.ungdomsytelse.UngdomsytelseSatsTypeKodeverdiConverter;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsytelsePeirodeEndringTypeKodeverdiConverter;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsytelsePeriodeEndringType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.Objects;

import static no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode.SøktPeriodeData;

@Entity(name = "UngdomsytelseSøktStartdato")
@Table(name = "UNG_BEKREFTET_PERIODE_ENDRING")
@Immutable
public class UngdomsytelseBekreftetPeriodeEndring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKT_STARTDATO")
    private Long id;

    @Convert(converter = UngdomsytelsePeirodeEndringTypeKodeverdiConverter.class)
    @Column(name = "endring_type", nullable = false)
    private UngdomsytelsePeriodeEndringType endringType;

    @Column(name = "dato", nullable = false)
    private LocalDate dato;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseBekreftetPeriodeEndring(LocalDate dato, JournalpostId journalpostId, UngdomsytelsePeriodeEndringType endringType) {
        this.dato = dato;
        this.journalpostId = journalpostId;
        this.endringType = endringType;
    }

    public UngdomsytelseBekreftetPeriodeEndring(LocalDate startdato) {
        this.dato = startdato;
    }

    public UngdomsytelseBekreftetPeriodeEndring(UngdomsytelseBekreftetPeriodeEndring it) {
        this.journalpostId = it.getJournalpostId();
        this.dato = it.getDato();
    }

    public UngdomsytelseBekreftetPeriodeEndring() {
        // hibernate
    }

    public LocalDate getDato() {
        return dato;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public UngdomsytelsePeriodeEndringType getEndringType() {
        return endringType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseBekreftetPeriodeEndring that = (UngdomsytelseBekreftetPeriodeEndring) o;
        return Objects.equals(dato, that.dato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dato);
    }

    @Override
    public String toString() {
        return "UngdomsytelseSøktStartdato{" +
            "id=" + id +
            ", startdato=" + dato +
            ", journalpostId=" + journalpostId +
            ", versjon=" + versjon +
            '}';
    }
}
