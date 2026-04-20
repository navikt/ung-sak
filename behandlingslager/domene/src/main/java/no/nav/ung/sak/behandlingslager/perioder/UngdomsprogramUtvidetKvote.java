package no.nav.ung.sak.behandlingslager.perioder;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity(name = "UngdomsprogramUtvidetKvote")
@Table(name = "UNG_UNGDOMSPROGRAM_UTVIDET_KVOTE")
@Immutable
public class UngdomsprogramUtvidetKvote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UNGDOMSPROGRAM_UTVIDET_KVOTE_ID")
    private Long id;

    @Column(name = "har_utvidet_kvote", nullable = false)
    private boolean harUtvidetKvote;

    public UngdomsprogramUtvidetKvote() {
    }

    public UngdomsprogramUtvidetKvote(Long id, boolean harUtvidetKvote) {
        this.id = id;
        this.harUtvidetKvote = harUtvidetKvote;
    }

    public Long getId() {
        return id;
    }

    public boolean isHarUtvidetKvote() {
        return harUtvidetKvote;
    }
}
