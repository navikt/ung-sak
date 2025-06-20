package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.JournalpostId;

@Entity(name = "Uttalelse")
@Table(name = "UTTALELSE")
public class UttalelseEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTALELSE")
    private Long id;

    @Column(name = "uttalelse_begrunnelse", updatable = false)
    private String uttalelseBegrunnelse;

    @Column(name = "har_uttalelse", updatable = false, nullable = false)
    private boolean harUttalelse;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "svar_journalpost_id")))
    private JournalpostId svarJournalpostId;

    public UttalelseEntitet() {
        // Hibernate
    }

    public UttalelseEntitet(boolean harUttalelse, String uttalelseBegrunnelse, JournalpostId svarJournalpostId) {
        this.uttalelseBegrunnelse = uttalelseBegrunnelse;
        this.harUttalelse = harUttalelse;
        this.svarJournalpostId = svarJournalpostId;
    }

    @Override
    public String toString() {
        return "UttalelseEntitet{" +
            "id=" + id +
            ", harUttalelse=" + harUttalelse +
            ", svarJournalpostId=" + svarJournalpostId +
            '}';
    }

    public String getUttalelseBegrunnelse() {
        return uttalelseBegrunnelse;
    }

    public boolean harUttalelse() {
        return harUttalelse;
    }

    public JournalpostId getSvarJournalpostId() {
        return svarJournalpostId;
    }
}
