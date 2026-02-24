package no.nav.ung.sak.oppgave;

import jakarta.persistence.*;
import no.nav.ung.sak.BaseEntitet;
import no.nav.ung.sak.kontrakt.oppgaver.BekreftelseDTO;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.oppgave.typer.OppgaveDataEntitet;
import no.nav.ung.sak.typer.AktørId;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "BrukerdialogOppgave")
@Table(name = "BD_OPPGAVE")
public class BrukerdialogOppgaveEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BD_OPPGAVE")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false, updatable = false)))
    private AktørId aktørId;

    @Column(name = "oppgavereferanse", nullable = false, updatable = false, unique = true)
    private UUID oppgavereferanse;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OppgaveStatus status = OppgaveStatus.ULØST;

    @Convert(converter = OppgaveTypeKodeverdiConverter.class)
    @Column(name = "type")
    private OppgaveType oppgaveType;

    @Column(name = "frist_tid")
    private LocalDateTime fristTid;

    @Column(name = "løst_dato")
    private LocalDateTime løstDato; // NOSONAR

    @Column(name = "åpnet_dato")
    private LocalDateTime åpnetDato; // NOSONAR

    @Column(name = "lukket_dato")
    private LocalDateTime lukketDato; // NOSONAR

    @Convert(converter = OppgaveBekreftelseConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "bekreftelse", columnDefinition = "jsonb")
    private BekreftelseDTO bekreftelse;

    @OneToOne(mappedBy = "oppgave", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private OppgaveDataEntitet oppgaveData;

    protected BrukerdialogOppgaveEntitet() {
        // For JPA
    }

    public BrukerdialogOppgaveEntitet(UUID oppgavereferanse,
                                      OppgaveType oppgaveType,
                                      AktørId aktørId,
                                      LocalDateTime fristTid) {
        this.oppgavereferanse = oppgavereferanse;
        this.oppgaveType = oppgaveType;
        this.aktørId = aktørId;
        this.fristTid = fristTid;
    }

    /**
     * Konstruktør for migrering av oppgave fra annen applikasjon.
     * Brukes når alle felter inkludert status og datoer skal settes.
     */
    public BrukerdialogOppgaveEntitet(UUID oppgavereferanse,
                                      OppgaveType oppgaveType,
                                      AktørId aktørId,
                                      BekreftelseDTO bekreftelse,
                                      OppgaveStatus status,
                                      LocalDateTime fristTid,
                                      LocalDateTime løstDato,
                                      LocalDateTime åpnetDato,
                                      LocalDateTime lukketDato) {
        this.oppgavereferanse = oppgavereferanse;
        this.oppgaveType = oppgaveType;
        this.aktørId = aktørId;
        this.bekreftelse = bekreftelse;
        this.status = status;
        this.fristTid = fristTid;
        this.løstDato = løstDato;
        this.åpnetDato = åpnetDato;
        this.lukketDato = lukketDato;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public UUID getOppgavereferanse() {
        return oppgavereferanse;
    }

    public OppgaveStatus getStatus() {
        return status;
    }

    public OppgaveType getOppgaveType() {
        return oppgaveType;
    }


    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    protected void setStatus(OppgaveStatus status) {
        this.status = status;
    }

    public void setLøstDato(LocalDateTime løstDato) {
        this.løstDato = løstDato;
    }

    public LocalDateTime getLøstDato() {
        return løstDato;
    }

    public void setÅpnetDato(LocalDateTime åpnetDato) {
        this.åpnetDato = åpnetDato;
    }

    public LocalDateTime getÅpnetDato() {
        return åpnetDato;
    }

    public void setLukketDato(LocalDateTime lukketDato) {
        this.lukketDato = lukketDato;
    }

    public LocalDateTime getLukketDato() {
        return lukketDato;
    }

    public BekreftelseDTO getBekreftelse() {
        return bekreftelse;
    }

    public void setBekreftelse(BekreftelseDTO bekreftelse) {
        this.bekreftelse = bekreftelse;
    }

    Long getId() {
        return id;
    }

    public OppgaveDataEntitet getOppgaveData() {
        return oppgaveData;
    }

    public void setOppgaveData(OppgaveDataEntitet oppgaveData) {
        this.oppgaveData = oppgaveData;
        if (oppgaveData != null) {
            oppgaveData.setOppgave(this);
        }
    }
}
