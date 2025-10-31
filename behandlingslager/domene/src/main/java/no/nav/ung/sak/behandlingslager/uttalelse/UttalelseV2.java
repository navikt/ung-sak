package no.nav.ung.sak.behandlingslager.uttalelse;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.kodeverk.EndringTypeKodeverdiConverter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.hibernate.annotations.Immutable;

import java.util.Objects;
import java.util.UUID;

@Entity(name = "UttalelseV2")
@Table(name = "UTTALELSE_V2")
@Immutable
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

    @Column(name = "endring_type_kode", nullable = false)
    @Convert(converter = EndringTypeKodeverdiConverter.class)
    private EndringType type;

    @Column(name = "grunnlag_ref", nullable = false)
    private UUID grunnlagsreferanse;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "svar_journalpost_id")))
    private JournalpostId svarJournalpostId;

    public UttalelseV2() {
        // Hibernate
    }

    public UttalelseV2(UttalelseV2 eksisterende) {
        this.uttalelseBegrunnelse = eksisterende.uttalelseBegrunnelse;
        this.harUttalelse = eksisterende.harUttalelse;
        this.periode = eksisterende.periode;
        this.svarJournalpostId = eksisterende.svarJournalpostId;
        this.type = eksisterende.type;
        this.grunnlagsreferanse = eksisterende.grunnlagsreferanse;
    }


    public UttalelseV2(boolean harUttalelse, String uttalelseBegrunnelse, DatoIntervallEntitet periode, JournalpostId svarJournalpostId, EndringType type, UUID grunnlagsreferanse) {
        this.uttalelseBegrunnelse = uttalelseBegrunnelse;
        this.harUttalelse = harUttalelse;
        this.periode = periode;
        this.svarJournalpostId = svarJournalpostId;
        this.type = type;
        this.grunnlagsreferanse = grunnlagsreferanse;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UttalelseV2 that = (UttalelseV2) o;
        return harUttalelse == that.harUttalelse &&
            Objects.equals(uttalelseBegrunnelse, that.uttalelseBegrunnelse) &&
            Objects.equals(periode, that.periode) &&
            type == that.type &&
            Objects.equals(grunnlagsreferanse, that.grunnlagsreferanse) &&
            Objects.equals(svarJournalpostId, that.svarJournalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uttalelseBegrunnelse, harUttalelse, periode, type, grunnlagsreferanse, svarJournalpostId);
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

    public Long getId() {
        return id;
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

    public UUID getGrunnlagsreferanse() {
        return grunnlagsreferanse;
    }

    public EndringType getType() {
        return type;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
}
