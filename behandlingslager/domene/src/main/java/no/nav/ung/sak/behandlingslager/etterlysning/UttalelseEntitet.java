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

    @Column(name = "uttalelse", updatable = false)
    private String uttalelseTekst;

    @Column(name = "er_endringen_godkjent", updatable = false, nullable = false)
    private boolean erEndringenGodkjent;

    @Column(name = "etterlysning_id", updatable = false, nullable = false)
    private long etterlysningId;


    private UttalelseEntitet() {
        // Hibernate
    }

    public UttalelseEntitet(long etterlysningId, boolean erEndringenGodkjent, String uttalelseTekst) {
        this.uttalelseTekst = uttalelseTekst;
        this.erEndringenGodkjent = erEndringenGodkjent;
        this.etterlysningId = etterlysningId;
    }



    public String getUttalelseTekst() {
        return uttalelseTekst;
    }

    public boolean erEndringenGodkjent() {
        return erEndringenGodkjent;
    }
}
