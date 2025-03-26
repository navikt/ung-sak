package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

@Entity(name = "Uttalelse")
@Table(name = "UTTALELSE")
@Immutable
public class UttalelseEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTALELSE")
    private Long id;

    @Column(name = "uttalelse_begrunnelse", updatable = false)
    private String uttalelseBegrunnelse;

    @Column(name = "har_godtatt_endringen", updatable = false, nullable = false)
    private boolean harGodtattEndringen;

    @Column(name = "etterlysning_id", updatable = false, nullable = false)
    private long etterlysningId;


    private UttalelseEntitet() {
        // Hibernate
    }

    public UttalelseEntitet(long etterlysningId, boolean harGodtattEndringen, String uttalelseBegrunnelse) {
        this.uttalelseBegrunnelse = uttalelseBegrunnelse;
        this.harGodtattEndringen = harGodtattEndringen;
        this.etterlysningId = etterlysningId;
    }



    public String getUttalelseBegrunnelse() {
        return uttalelseBegrunnelse;
    }

    public boolean harGodtattEndringen() {
        return harGodtattEndringen;
    }
}
