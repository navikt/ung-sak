package no.nav.k9.sak.ytelse.opplaeringspenger.repo;

import java.util.ArrayList;
import java.util.List;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;

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

    @OneToMany
    @JoinTable(
        name="OLP_VURDERT_OPPLAERING_ANVENDT_DOKUMENT",
        joinColumns = @JoinColumn( name="VURDERT_OPPLAERING_ID"),
        inverseJoinColumns = @JoinColumn( name="PLEIETRENGENDE_SYKDOM_DOKUMENT_ID")
    )
    private List<PleietrengendeSykdomDokument> dokumenter = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertOpplæring() {
    }

    public VurdertOpplæring(JournalpostId journalpostId, Boolean nødvendigOpplæring, String begrunnelse, List<PleietrengendeSykdomDokument> dokumenter) {
        this.journalpostId = journalpostId;
        this.nødvendigOpplæring = nødvendigOpplæring;
        this.begrunnelse = begrunnelse;
        this.dokumenter = new ArrayList<>(dokumenter);
    }

    public VurdertOpplæring(VurdertOpplæring that) {
        this.journalpostId = that.journalpostId;
        this.nødvendigOpplæring = that.nødvendigOpplæring;
        this.begrunnelse = that.begrunnelse;
        this.dokumenter = new ArrayList<>(that.dokumenter);
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

    public List<PleietrengendeSykdomDokument> getDokumenter() {
        return dokumenter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertOpplæring that = (VurdertOpplæring) o;
        return Objects.equals(nødvendigOpplæring, that.nødvendigOpplæring)
            && Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(dokumenter, that.dokumenter)
            && Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, nødvendigOpplæring, begrunnelse, dokumenter);
    }

    @Override
    public String toString() {
        return "VurdertOpplæring{" +
            "journalpostId=" + journalpostId +
            ", nødvendigOpplæring=" + nødvendigOpplæring +
            ", dokumenter=" + dokumenter +
            ", begrunnelse=" + begrunnelse +
            '}';
    }
}
