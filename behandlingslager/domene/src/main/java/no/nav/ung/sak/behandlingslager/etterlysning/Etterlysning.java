package no.nav.ung.sak.behandlingslager.etterlysning;

import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
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

    @Column(name = "frist")
    private LocalDateTime frist;

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

    public UUID getGrunnlagsreferanse() {
        return grunnlagsreferanse;
    }

    public UUID getEksternReferanse() {
        return eksternReferanse;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public EtterlysningType getType() {
        return type;
    }

    public EtterlysningStatus getStatus() {
        return status;
    }

    public void vent(LocalDateTime frist) {
        if (status != EtterlysningStatus.OPPRETTET) {
            throw new IllegalStateException("Kan vente på etterlysning som ikke er satt til OPPRETTET. Status er " + status);
        }
        this.status = EtterlysningStatus.VENTER;
        this.frist = frist;
    }

    public void avbryt() {
        if (status != EtterlysningStatus.SKAL_AVBRYTES) {
            throw new IllegalStateException("Kan ikke avbryte etterlysning som ikke er satt til SKAL_AVBRYTES. Status er " + status);
        }
        this.status = EtterlysningStatus.AVBRUTT;
        this.frist = null;
    }


    public void skalAvbrytes() {
        if (status == EtterlysningStatus.MOTTATT_SVAR) {
            throw new IllegalStateException("Kan ikke avbryte etterlysning som er mottatt.");
        }
        this.status = EtterlysningStatus.SKAL_AVBRYTES;
    }

}
