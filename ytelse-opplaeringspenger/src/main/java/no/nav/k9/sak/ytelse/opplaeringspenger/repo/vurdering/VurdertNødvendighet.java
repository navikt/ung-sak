package no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering;

import java.time.LocalDateTime;
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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;

@Entity(name = "VurdertNødvendighet")
@Table(name = "olp_vurdert_noedvendighet")
@Immutable
public class VurdertNødvendighet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_NOEDVENDIGHET")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Column(name = "noedvendig_opplaering", nullable = false)
    private Boolean nødvendigOpplæring = false;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Column(name = "vurdert_av", nullable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tid", nullable = false)
    private LocalDateTime vurdertTidspunkt;

    @OneToMany
    @JoinTable(
        name="OLP_VURDERT_NOEDVENDIGHET_ANVENDT_DOKUMENT",
        joinColumns = @JoinColumn( name="VURDERT_NOEDVENDIGHET_ID"),
        inverseJoinColumns = @JoinColumn( name="OPPLAERING_DOKUMENT_ID")
    )
    private List<OpplæringDokument> dokumenter = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertNødvendighet() {
    }

    public VurdertNødvendighet(JournalpostId journalpostId, Boolean nødvendigOpplæring, String begrunnelse, String vurdertAv, LocalDateTime vurdertTidspunkt, List<OpplæringDokument> dokumenter) {
        this.journalpostId = journalpostId;
        this.nødvendigOpplæring = nødvendigOpplæring;
        this.begrunnelse = begrunnelse;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
        this.dokumenter = new ArrayList<>(dokumenter);
    }

    public VurdertNødvendighet(VurdertNødvendighet that) {
        this.journalpostId = that.journalpostId;
        this.nødvendigOpplæring = that.nødvendigOpplæring;
        this.begrunnelse = that.begrunnelse;
        this.vurdertAv = that.vurdertAv;
        this.vurdertTidspunkt = that.vurdertTidspunkt;
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

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }

    public List<OpplæringDokument> getDokumenter() {
        return new ArrayList<>(dokumenter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VurdertNødvendighet that = (VurdertNødvendighet) o;
        return Objects.equals(nødvendigOpplæring, that.nødvendigOpplæring)
            && Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(dokumenter, that.dokumenter)
            && Objects.equals(begrunnelse, that.begrunnelse)
            && Objects.equals(vurdertAv, that.vurdertAv)
            && Objects.equals(vurdertTidspunkt, that.vurdertTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, nødvendigOpplæring, begrunnelse, vurdertAv, vurdertTidspunkt, dokumenter);
    }

    @Override
    public String toString() {
        return "VurdertNødvendighet{" +
            "journalpostId=" + journalpostId +
            ", nødvendigOpplæring=" + nødvendigOpplæring +
            ", dokumenter=" + dokumenter +
            ", begrunnelse=" + begrunnelse +
            ", vurdertAv=" + vurdertAv +
            ", vurdertTidspunkt=" + vurdertTidspunkt +
            '}';
    }
}
