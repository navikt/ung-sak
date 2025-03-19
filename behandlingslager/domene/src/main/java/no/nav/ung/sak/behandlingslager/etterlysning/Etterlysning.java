package no.nav.ung.sak.behandlingslager.etterlysning;

import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "Etterlysning")
@Table(name = "ETTERLYSNING")
@Immutable
public class Etterlysning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETTERLYSNING")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "grunnlag_ref", nullable = false)
    private UUID grunnlagsreferanse;

    @Column(name = "ekstern_ref", nullable = false)
    private UUID eksternReferanse;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "type", nullable = false)
    private EtterlysningType type;

    @Column(name = "status", nullable = false)
    private EtterlysningStatus status;

    private Etterlysning() {
        // Hibernate
    }

    public static Etterlysning forInntektKontrollUttalelse(
        Long behandlingId,
        UUID grunnlagsreferanse,
        UUID eksternReferanse,
        DatoIntervallEntitet periode) {

        return new Etterlysning(
            behandlingId,
            grunnlagsreferanse,
            eksternReferanse,
            periode,
            EtterlysningType.UTTALELSE_KONTROLL_INNTEKT,
            EtterlysningStatus.OPPRETTET);
    }

    public Etterlysning(Long behandlingId,
                        UUID grunnlagsreferanse,
                        UUID eksternReferanse,
                        DatoIntervallEntitet periode,
                        EtterlysningType type,
                        EtterlysningStatus status) {
        this.behandlingId = behandlingId;
        this.grunnlagsreferanse = grunnlagsreferanse;
        this.eksternReferanse = eksternReferanse;
        this.periode = periode;
        this.type = type;
        this.status = status;
    }

    public Long getId() {
        return id;
    }
}
