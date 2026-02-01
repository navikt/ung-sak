package no.nav.ung.fordel.repo.journalpost;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;

@Entity(name = "JournalpostMottattEntitet")
@Table(name = "FORDEL_JOURNALPOST_MOTTATT")
public class JournalpostMottattEntitet {

    public enum Status {
        UBEHANDLET("UBEHANDLET"),
        BEHANDLET("BEHANDLET");

        private final String dbKode;

        private Status(String dbKode) {
            this.dbKode = dbKode;
        }

        public String getDbKode() {
            return dbKode;
        }
    }

    @Id
    @Column(name = "journalpost_id")
    private String journalpostId;

    @Column(name = "aktoer_id")
    private String aktørId;

    @Column(name = "status", nullable = false)
    private String status = Status.UBEHANDLET.getDbKode();

    @Column(name = "payload")
    private String payload;

    @Column(name = "brevkode")
    private String brevkode;

    /** Innsendingstidspunkt angitt i melding eller mottatt fra kildesystem (eks. Altinn). */
    @Column(name = "mottatt_tidspunkt", nullable = false)
    private LocalDateTime mottattTidspunkt;

    @Column(name = "opprettet_tid", nullable = false, updatable = false, insertable = false)
    private LocalDateTime opprettetTid;

    @Column(name = "endret_tid", nullable = true, insertable = false, updatable = true)
    private LocalDateTime endretTid;

    @Column(name = "behandling_tema")
    private String behandlingTema;

    @Column(name = "tittel")
    private String tittel;

    JournalpostMottattEntitet() {
        // for hibernate
    }

    public JournalpostMottattEntitet(JournalpostId journalpostId,
                                     BehandlingTema behandlingTema,
                                     AktørId aktørId,
                                     String brevkode,
                                     LocalDateTime mottattTidspunkt,
                                     String tittel,
                                     String payload,
                                     Status status) {
        this.behandlingTema = behandlingTema == null? null: behandlingTema.getOffisiellKode();
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId").getVerdi();
        this.aktørId = aktørId == null ? null : aktørId.getId();
        this.tittel = tittel;
        this.payload = payload;
        this.brevkode = brevkode;
        this.mottattTidspunkt = Objects.requireNonNull(mottattTidspunkt, "innsendingstidspunkt");
        this.status = Objects.requireNonNull(status, "status").getDbKode();
    }

    @PreUpdate
    protected void onUpdate() {
        this.endretTid = LocalDateTime.now();
    }

    public JournalpostId getJournalpostId() {
        return new JournalpostId(journalpostId);
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getOpprettetTid() {
        return opprettetTid;
    }

    public BehandlingTema getBehandlingTema() {
        return BehandlingTema.fraKode(behandlingTema);
    }

    public LocalDateTime getEndretTid() {
        return endretTid;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public String getTittel() {
        return tittel;
    }

    public Brevkode getBrevkode() {
        return Brevkode.fraKode(brevkode);
    }

    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status, "status").getDbKode();
    }

    public String getAktørId() {
        return aktørId;
    }
}
