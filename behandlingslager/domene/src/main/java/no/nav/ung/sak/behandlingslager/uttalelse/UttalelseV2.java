package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;

import java.util.Objects;


@Entity(name = "UttalelseV2")
@Table(name = "UTTALELSE_V2")
public class UttalelseV2 extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTALELSE_V2")
    private Long id;

    @Column(name = "begrunnelse", updatable = false)
    private String uttalelseBegrunnelse;

    @Column(name = "har_uttalelse", updatable = false, nullable = false)
    private boolean harUttalelse;

    @Embedded
    private DatoIntervallEntitet periode;

    @Column(name = "endring_type", nullable = false)
    private EndringType type;

    @Column(name = "grunnlag_ref", nullable = false)
    private Long grunnlagsreferanse;


    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "svar_journalpost_id")))
    private JournalpostId svarJournalpostId;

    public UttalelseV2() {
        // Hibernate
    }

    public UttalelseV2(boolean harUttalelse, String uttalelseBegrunnelse, DatoIntervallEntitet periode, JournalpostId svarJournalpostId, EndringType type, Long grunnlagsreferanse) {
        this.uttalelseBegrunnelse = uttalelseBegrunnelse;
        this.harUttalelse = harUttalelse;
        this.periode = periode;
        this.svarJournalpostId = svarJournalpostId;
        this.type = type;
        this.grunnlagsreferanse = grunnlagsreferanse;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UttalelseV2 that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "UttalelseV2{" +
            "harUttalelse=" + harUttalelse +
            ", periode=" + periode +
            ", svarJournalpostId=" + svarJournalpostId +
            ", type=" + type +
            ", grunnlagsreferanse=" + grunnlagsreferanse +
            ", id=" + id +
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

    public Long getGrunnlagsreferanse() {
        return grunnlagsreferanse;
    }

    public EndringType getType() {
        return type;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
}
