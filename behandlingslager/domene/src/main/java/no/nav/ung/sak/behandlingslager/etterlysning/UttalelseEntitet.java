package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

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

    //Hibernate - skal brukes via Etterlysning, men hibernate trenger dette for å kunne hente fra databasen sammen med Etterlysning
    @OneToOne
    @JoinColumn(name = "etterlysning_id", referencedColumnName = "id", nullable = false)
    private Etterlysning etterlysning;


    private UttalelseEntitet() {
        // Hibernate
    }

    public UttalelseEntitet(Etterlysning etterlysningId, boolean harGodtattEndringen, String uttalelseBegrunnelse) {
        this.uttalelseBegrunnelse = uttalelseBegrunnelse;
        this.harGodtattEndringen = harGodtattEndringen;
        this.etterlysning = etterlysningId;
    }

    @Override
    public String toString() {
        return "UttalelseEntitet{" +
            "id=" + id +
            ", harGodtattEndringen=" + harGodtattEndringen +
            ", etterlysningId=" + etterlysning.getId() +
            '}';
    }

    public String getUttalelseBegrunnelse() {
        return uttalelseBegrunnelse;
    }

    public boolean harGodtattEndringen() {
        return harGodtattEndringen;
    }
}
