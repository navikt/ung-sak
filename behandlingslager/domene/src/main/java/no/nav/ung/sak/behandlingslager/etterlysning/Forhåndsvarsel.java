package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningStatus;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Entity(name = "Forhåndsvarsel")
@Table(name = "FORHANDSVARSEL")
public class Forhåndsvarsel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FORHANDSVARSEL")
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
    private EtterlysningType type;

    @Column(name = "status", nullable = false)
    private EtterlysningStatus status;

    @Column(name = "frist")
    private LocalDateTime frist;

    @OneToOne
    @JoinColumn(name = "uttalelse_id", unique = true)
    private UttalelseEntitet uttalelse;

    Forhåndsvarsel() {
        // Hibernate
    }

    public static Forhåndsvarsel forInntektKontrollUttalelse(
        Long behandlingId,
        UUID grunnlagsreferanse,
        UUID eksternReferanse,
        DatoIntervallEntitet periode) {

        return new Forhåndsvarsel(
            behandlingId,
            grunnlagsreferanse,
            eksternReferanse,
            periode,
            EtterlysningType.UTTALELSE_KONTROLL_INNTEKT,
            EtterlysningStatus.OPPRETTET);
    }

    public static Forhåndsvarsel opprettForType(
        Long behandlingId,
        UUID grunnlagsreferanse,
        UUID eksternReferanse,
        DatoIntervallEntitet periode,
        EtterlysningType type
    ) {
        return new Forhåndsvarsel(
            behandlingId,
            grunnlagsreferanse,
            eksternReferanse,
            periode,
            type,
            EtterlysningStatus.OPPRETTET
        );
    }

    public Forhåndsvarsel(Long behandlingId,
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

    public Forhåndsvarsel(Forhåndsvarsel other) {
        this.behandlingId = other.behandlingId;
        this.grunnlagsreferanse = other.grunnlagsreferanse;
        this.eksternReferanse = other.eksternReferanse;
        this.periode = other.periode;
        this.type = other.type;
        this.status = other.status;
        this.frist = other.frist;
        this.uttalelse = other.uttalelse;
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
            ", uttalelse=" + uttalelse +
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


    public void mottaSvar(JournalpostId svarJournalpostId, boolean harUttalelse, String uttalelse) {
        if (status != EtterlysningStatus.VENTER) {
            throw new IllegalStateException("Kan ikke motta svar på etterlysning som ikke er satt til VENTER. Status er " + status);
        }
        this.status = EtterlysningStatus.MOTTATT_SVAR;
        this.uttalelse = new UttalelseEntitet(harUttalelse, uttalelse, svarJournalpostId);
    }

    public Optional<UttalelseEntitet> getUttalelse() {
        return Optional.ofNullable(uttalelse);
    }
}
