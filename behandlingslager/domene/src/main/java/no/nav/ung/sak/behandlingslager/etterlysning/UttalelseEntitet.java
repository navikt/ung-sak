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

    @Column(name = "har_godtatt_endringen", updatable = false, nullable = false)
    private boolean harGodtattEndringen;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "svar_journalpost_id")))
    private JournalpostId svarJournalpostId;


    //Hibernate - skal brukes via Etterlysning, men hibernate trenger dette for å kunne hente fra databasen sammen med Etterlysning
    @OneToOne
    @JoinColumn(name = "etterlysning_id", referencedColumnName = "id", nullable = false)
    private Etterlysning etterlysning;


    private UttalelseEntitet() {
        // Hibernate
    }

    public UttalelseEntitet(Etterlysning etterlysningId, boolean harGodtattEndringen, String uttalelseBegrunnelse, JournalpostId svarJournalpostId) {
        this.uttalelseBegrunnelse = uttalelseBegrunnelse;
        this.harGodtattEndringen = harGodtattEndringen;
        this.etterlysning = etterlysningId;
        this.svarJournalpostId = svarJournalpostId;
    }

    @Override
    public String toString() {
        return "UttalelseEntitet{" +
            "id=" + id +
            ", harGodtattEndringen=" + harGodtattEndringen +
            ", svarJournalpostId=" + svarJournalpostId +
            ", etterlysningId=" + etterlysning.getId() +
            '}';
    }

    public String getUttalelseBegrunnelse() {
        return uttalelseBegrunnelse;
    }

    public boolean harGodtattEndringen() {
        return harGodtattEndringen;
    }

    public JournalpostId getSvarJournalpostId() {
        return svarJournalpostId;
    }
}
