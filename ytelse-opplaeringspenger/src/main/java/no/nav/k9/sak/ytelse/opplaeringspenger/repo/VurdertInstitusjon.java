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

@Entity(name = "VurdertInstitusjon")
@Table(name = "olp_vurdert_institusjon")
@Immutable
public class VurdertInstitusjon extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_INSTITUSJON")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Column(name = "godkjent", nullable = false)
    private Boolean godkjent = false;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertInstitusjon() {
    }

    public VurdertInstitusjon(JournalpostId journalpostId, Boolean godkjent, String begrunnelse) {
        this.journalpostId = journalpostId;
        this.godkjent = godkjent;
        this.begrunnelse = begrunnelse;
    }

    public VurdertInstitusjon(VurdertInstitusjon vurdertInstitusjon) {
        this.journalpostId = vurdertInstitusjon.journalpostId;
        this.godkjent = vurdertInstitusjon.godkjent;
        this.begrunnelse = vurdertInstitusjon.begrunnelse;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Boolean getGodkjent() {
        return godkjent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertInstitusjon that = (VurdertInstitusjon) o;
        return Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(godkjent, that.godkjent)
            && Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, godkjent, begrunnelse);
    }

    @Override
    public String toString() {
        return "VurdertInstitusjon{" +
            "journalpostId=" + journalpostId +
            ", godkjent=" + godkjent +
            ", begrunnelse=" + begrunnelse +
            '}';
    }
}
