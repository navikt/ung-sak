package no.nav.ung.sak.behandlingslager.behandling.startdato;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsprogramPeriodeEndringTypeKodeverdiConverter;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsprogramPeriodeEndringType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "UngdomsytelseBekreftetPeriodeEndring")
@Table(name = "UNG_BEKREFTET_PERIODE_ENDRING")
@Immutable
public class UngdomsprogramBekreftetPeriodeEndring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_BEKREFTET_PERIODE_ENDRING")
    private Long id;

    @Convert(converter = UngdomsprogramPeriodeEndringTypeKodeverdiConverter.class)
    @Column(name = "endring_type", nullable = false)
    private UngdomsprogramPeriodeEndringType endringType;

    @Column(name = "dato", nullable = false)
    private LocalDate dato;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsprogramBekreftetPeriodeEndring(LocalDate dato, JournalpostId journalpostId, UngdomsprogramPeriodeEndringType endringType) {
        this.dato = dato;
        this.journalpostId = journalpostId;
        this.endringType = endringType;
    }

    public UngdomsprogramBekreftetPeriodeEndring() {
        // hibernate
    }

    public LocalDate getDato() {
        return dato;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public UngdomsprogramPeriodeEndringType getEndringType() {
        return endringType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsprogramBekreftetPeriodeEndring that = (UngdomsprogramBekreftetPeriodeEndring) o;
        return Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() {
        return "UngdomsprogramBekreftetPeriodeEndring{" +
            "endringType=" + endringType +
            ", dato=" + dato +
            ", journalpostId=" + journalpostId +
            '}';
    }
}
