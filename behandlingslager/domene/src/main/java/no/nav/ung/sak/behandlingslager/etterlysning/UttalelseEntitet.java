package no.nav.ung.sak.behandlingslager.etterlysning;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "Uttalelse")
@Table(name = "UTTALELSE")
@Immutable
public class UttalelseEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTALELSE")
    private Long id;

    @Column(name = "uttalelse", updatable = false, nullable = false)
    private String uttalelseTekst;

    @Column(name = "etterlysning_id", updatable = false, nullable = false)
    private long etterlysningId;


    private UttalelseEntitet() {
        // Hibernate
    }

    public UttalelseEntitet(String uttalelseTekst, long etterlysningId) {
        this.uttalelseTekst = uttalelseTekst;
        this.etterlysningId = etterlysningId;
    }


    public String getUttalelseTekst() {
        return uttalelseTekst;
    }

}
