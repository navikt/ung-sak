package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

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
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "VurdertOpplæring")
@Table(name = "olp_vurdert_opplaering")
@Immutable
public class VurdertOpplæring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_OPPLAERING")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Column(name = "noedvendig_opplaering", nullable = false)
    private Boolean nødvendigOpplæring = false;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertOpplæring() {
    }

    public VurdertOpplæring(JournalpostId journalpostId, Boolean nødvendigOpplæring, String begrunnelse) {
        this.journalpostId = journalpostId;
        this.nødvendigOpplæring = nødvendigOpplæring;
        this.begrunnelse = begrunnelse;
    }

    public VurdertOpplæring(VurdertOpplæring that) {
        this.journalpostId = that.journalpostId;
        this.nødvendigOpplæring = that.nødvendigOpplæring;
        this.begrunnelse = that.begrunnelse;
    }

    public Boolean getNødvendigOpplæring() {
        return nødvendigOpplæring;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertOpplæring that = (VurdertOpplæring) o;
        return Objects.equals(nødvendigOpplæring, that.nødvendigOpplæring)
            && Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, nødvendigOpplæring, begrunnelse);
    }

    @Override
    public String toString() {
        return "VurdertOpplæring{" +
            "journalpostId=" + journalpostId +
            ", nødvendigOpplæring=" + nødvendigOpplæring +
            ", begrunnelse=" + begrunnelse +
            '}';
    }
}
