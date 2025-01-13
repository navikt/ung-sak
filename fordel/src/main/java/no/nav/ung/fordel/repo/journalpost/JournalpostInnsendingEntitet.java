package no.nav.ung.fordel.repo.journalpost;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserTypeLegacyBridge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;

@Entity(name = "JournalpostInnsendingEntitet")
@Table(name = "JOURNALPOST_INNSENDING")
public class JournalpostInnsendingEntitet {

    @Id
    @Column(name = "journalpost_id")
    private String journalpostId;
    @Column(name = "aktoer_id")
    private String aktørId;
    @Column(name = "saksnummer")
    private String saksnummer;
    @Column(name = "status")
    private String status = Status.UBEHANDLET.getDbKode();
    @Column(name = "ytelse_type")
    private String ytelseType;
    @Lob
    @Type(
            value = UserTypeLegacyBridge.class,
            parameters = @Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "org.hibernate.type.TextType")
    )
    @Column(name = "payload")
    private String payload;
    @Column(name = "brevkode")
    private String brevkode;
    /**
     * Innsendingstidspunkt angitt i melding eller mottatt fra kildesystem (eks. Altinn).
     */
    @Column(name = "innsendingstidspunkt")
    private LocalDateTime innsendingstidspunkt;
    @Column(name = "opprettet_tid", nullable = false, updatable = false, insertable = false)
    private LocalDateTime opprettetTid;
    @Column(name = "endret_tid", nullable = true, insertable = false, updatable = true)
    private LocalDateTime endretTid;

    JournalpostInnsendingEntitet() {
        // for hibernate
    }

    public JournalpostInnsendingEntitet(FagsakYtelseType ytelseType,
                                        Saksnummer saksnummer,
                                        JournalpostId journalpostId,
                                        AktørId aktørId,
                                        Brevkode brevkode,
                                        LocalDateTime innsendingstidspunkt,
                                        String payload, Status status) {
        this.ytelseType = Objects.requireNonNull(ytelseType, "ytelseType").getKode();
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId").getVerdi();
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId").getId();
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer").getVerdi();
        this.payload = payload;
        this.brevkode = Objects.requireNonNull(brevkode, "brevkode").getOffisiellKode();
        this.innsendingstidspunkt = Objects.requireNonNull(innsendingstidspunkt, "innsendingstidspunkt");
        this.status = Objects.requireNonNull(status, "status").getDbKode();
    }

    @PreUpdate
    protected void onUpdate() {
        this.endretTid = LocalDateTime.now();
    }

    public Saksnummer getSaksnummer() {
        return new Saksnummer(saksnummer);
    }

    public JournalpostId getJournalpostId() {
        return new JournalpostId(journalpostId);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status, "status").getDbKode();
    }


    public FagsakYtelseType getYtelseType() {
        return FagsakYtelseType.fraKode(ytelseType);
    }

    public LocalDateTime getOpprettetTid() {
        return opprettetTid;
    }

    public LocalDateTime getEndretTid() {
        return endretTid;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public Brevkode getBrevkode() {
        return Brevkode.fraKode(brevkode);
    }

    public String getAktørId() {
        return aktørId;
    }

    public enum Status {
        UBEHANDLET("UBEHANDLET"),
        INNSENDT("INNSENDT");

        private final String dbKode;

        private Status(String dbKode) {
            this.dbKode = dbKode;
        }

        public String getDbKode() {
            return dbKode;
        }
    }
}
