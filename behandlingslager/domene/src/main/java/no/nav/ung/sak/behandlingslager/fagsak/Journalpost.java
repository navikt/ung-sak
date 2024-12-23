package no.nav.ung.sak.behandlingslager.fagsak;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.JournalpostId;

@Entity(name = "Journalpost")
@Table(name = "JOURNALPOST")
public class Journalpost extends BaseEntitet {

    private static final String JOURNALPOSTID_PÅKREVD = "journalpostId er påkrevd";
    private static final String FAGSAK_PÅKREVD = "fagsak er påkrevd";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_JOURNALPOST")
    @Column(name = "id")
    private Long id;

    @Column(name = "journalpost_id", nullable = false, unique = true)
    private JournalpostId journalpostId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fagsak_id", nullable = false)
    private Fagsak fagsak;

    Journalpost() {
    }

    public Journalpost(JournalpostId journalpostId, Fagsak fagsak) {
        this.journalpostId = Objects.requireNonNull(journalpostId, JOURNALPOSTID_PÅKREVD);
        this.fagsak = Objects.requireNonNull(fagsak, FAGSAK_PÅKREVD);
    }

    public Journalpost(String journalpostId, Fagsak fagsak) {
        this.journalpostId = new JournalpostId(Objects.requireNonNull(journalpostId, JOURNALPOSTID_PÅKREVD));
        this.fagsak = Objects.requireNonNull(fagsak, FAGSAK_PÅKREVD);
    }


    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    public void knyttJournalpostTilFagsak(Fagsak fagsak) {
        Objects.requireNonNull(fagsak, FAGSAK_PÅKREVD);
        this.fagsak = fagsak;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Journalpost)) {
            return false;
        }
        Journalpost other = (Journalpost) obj;

        return Objects.equals(this.journalpostId, other.journalpostId)
                && Objects.equals(this.fagsak, other.fagsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, journalpostId);
    }
}
