package no.nav.ung.sak.behandlingslager.perioder;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.sak.behandlingslager.kodeverk.HjemmelKodeverdiConverter;
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

    @Convert(converter = HjemmelKodeverdiConverter.class)
    @Column(name = "hjemmel")
    private Hjemmel hjemmel = Hjemmel.UNG_FORSKRIFT_PARAGRAF_6;

    public UngdomsprogramUtvidetKvote() {
    }

    public UngdomsprogramUtvidetKvote(boolean harUtvidetKvote) {
        this.harUtvidetKvote = harUtvidetKvote;
    }

    public UngdomsprogramUtvidetKvote(boolean harUtvidetKvote, Hjemmel hjemmel) {
        this.harUtvidetKvote = harUtvidetKvote;
        this.hjemmel = hjemmel;
    }

    public Long getId() {
        return id;
    }

    public boolean isHarUtvidetKvote() {
        return harUtvidetKvote;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }
}
