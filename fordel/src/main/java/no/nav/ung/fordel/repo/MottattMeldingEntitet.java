package no.nav.ung.fordel.repo;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserTypeLegacyBridge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity(name = "MottattMelding")
@Table(name = "FORDEL_MOTTATT_MELDING")
@DynamicInsert
@DynamicUpdate
public class MottattMeldingEntitet {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.SEQUENCE, generator = "SEQ_FORDEL_MOTTATT_MELDING")
    @Column(name = "id")
    private Long id;

    @NaturalId
    @Column(name = "journalpostid", nullable = false, updatable = false)
    private String journalpostId;

    /**
     * Tema angitt fra joark.
     */
    @Column(name = "tema")
    private String tema;

    /**
     * Behandlingstema angitt fra joark.
     */
    @Column(name = "behandlingstema")
    private String behandlingstema;

    /**
     * Behandlingstype angitt fra joark.
     */
    @Column(name = "behandlingstype")
    private String behandlingstype;

    /**
     * Brevkode angitt fra joark.
     */
    @Column(name = "brevkode")
    private String brevkode;

    /**
     * Innsendt id dersom dette gjelder en søknad.
     */
    @Column(name = "soeknad_id")
    private String søknadId;

    @Column(name = "opprettet_tid", nullable = false, updatable = false, insertable = false)
    private LocalDateTime opprettetTid;

    @Column(name = "endret_tid", nullable = true, insertable = false, updatable = true)
    private LocalDateTime endretTid;

    @Lob
    @Type(
        value = UserTypeLegacyBridge.class,
        parameters = @Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "org.hibernate.type.TextType")
    )
    @Column(name = "payload")
    private String payload;

    MottattMeldingEntitet() {
        // for JPA
    }

    public MottattMeldingEntitet(String journalpostId) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
    }

    @PreUpdate
    protected void onUpdate() {
        this.endretTid = LocalDateTime.now();
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(String behandlingstema) {
        this.behandlingstema = behandlingstema;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(String behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public void setBrevkode(String brevkode) {
        this.brevkode = brevkode;
    }

    public String getSøknadId() {
        return søknadId;
    }

    public void setSøknadId(String søknadId) {
        this.søknadId = søknadId;
    }

    public Long getId() {
        return id;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String text) {
        this.payload = text;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof MottattMeldingEntitet))
            return false;
        var other = (MottattMeldingEntitet) obj;
        return Objects.equals(journalpostId, other.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() { // NOSONAR
        return getClass().getSimpleName() + "<"
            + "journalpostId=" + journalpostId
            + (tema == null ? "" : ", tema=" + tema)
            + (behandlingstema == null ? "" : ", behandlingstema=" + behandlingstema)
            + (behandlingstype == null ? "" : ", behandlingstype=" + behandlingstype)
            + (brevkode == null ? "" : ", brevkode=" + brevkode)
            + (søknadId == null ? "" : ", søknadId=" + søknadId)
            + ">";
    }

}
