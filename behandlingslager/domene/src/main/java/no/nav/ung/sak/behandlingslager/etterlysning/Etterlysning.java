package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.felles.BaseEntitet;
import no.nav.ung.sak.behandlingslager.kodeverk.EtterlysningStatusKodeverdiConverter;
import no.nav.ung.sak.behandlingslager.kodeverk.EtterlysningTypeKodeverdiConverter;
import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "Etterlysning")
@Table(name = "ETTERLYSNING")
public class Etterlysning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETTERLYSNING")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "grunnlag_ref", nullable = false)
    private UUID grunnlagsreferanse;

    /**
     * Referanse mot deltager-app oppgave styrt av ung-sak
     */
    @Column(name = "ekstern_ref", nullable = false)
    private UUID eksternReferanse;

    @Embedded
    private DatoIntervallEntitet periode;

    @Column(name = "type", nullable = false)
    @Convert(converter = EtterlysningTypeKodeverdiConverter.class)
    private EtterlysningType type;

    @Column(name = "status", nullable = false)
    @Convert(converter = EtterlysningStatusKodeverdiConverter.class)
    private EtterlysningStatus status;

    @Column(name = "frist")
    private LocalDateTime frist;

    public Etterlysning() {
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

    public static Etterlysning opprettForType(
        Long behandlingId,
        UUID grunnlagsreferanse,
        UUID eksternReferanse,
        DatoIntervallEntitet periode,
        EtterlysningType type
    ) {
        return new Etterlysning(
            behandlingId,
            grunnlagsreferanse,
            eksternReferanse,
            periode,
            type,
            EtterlysningStatus.OPPRETTET
        );
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

    @Override
    public String toString() {
        return "Etterlysning{" +
            "id=" + id +
            ", behandlingId=" + behandlingId +
            ", grunnlagsreferanse=" + grunnlagsreferanse +
            ", eksternReferanse=" + eksternReferanse +
            ", periode=" + periode +
            ", type=" + type +
            ", status=" + status +
            ", frist=" + frist +
            '}';
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

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDateTime getFrist() {
        return frist;
    }

    public void setFrist(LocalDateTime frist) {
        this.frist = frist;
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

    public void utløpt() {
        if (status != EtterlysningStatus.VENTER) {
            throw new IllegalStateException("Kan ikke avbryte etterlysning dersom status ikke er VENTER. Status var " + status);
        }
        this.status = EtterlysningStatus.UTLØPT;
    }


    public void mottaSvar() {
        if (status != EtterlysningStatus.VENTER) {
            throw new IllegalStateException("Kan ikke motta svar på etterlysning som ikke er satt til VENTER. Status er " + status);
        }
        this.status = EtterlysningStatus.MOTTATT_SVAR;
    }


}
