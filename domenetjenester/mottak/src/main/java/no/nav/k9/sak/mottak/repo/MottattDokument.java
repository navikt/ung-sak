package no.nav.k9.sak.mottak.repo;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.engine.jdbc.ClobProxy;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.BrevkodeKodeverdiConverter;
import no.nav.k9.sak.typer.JournalpostId;

/**
 * Entitetsklasse for mottatte dokument.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */

@Entity(name = "MottattDokument")
@Table(name = "MOTTATT_DOKUMENT")
@DynamicInsert
@DynamicUpdate
public class MottattDokument extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MOTTATT_DOKUMENT")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Column(name = "behandling_id", updatable = false)
    private Long behandlingId;

    @Column(name = "mottatt_dato", updatable = false)
    private LocalDate mottattDato;

    @Column(name = "mottatt_tidspunkt", updatable = false)
    private LocalDateTime mottattTidspunkt;

    @Column(name = "kanalreferanse", updatable = false)
    private String kanalreferanse;

    /**
     * Av historiske årsaker kalles dette kodeverkt for Brevkode her. Vi lagrer kun intern brevkode kode, så vi ikke er avhengig av brev i
     * fremtiden.
     */
    @Convert(converter = BrevkodeKodeverdiConverter.class)
    @Column(name = "type", updatable = false, nullable = false)
    private Brevkode type;

    @Lob
    @Column(name = "payload", updatable = false)
    private Clob payload;

    @Transient
    private String payloadString;

    @Column(name = "fagsak_id", nullable = false)
    private Long fagsakId;

    MottattDokument() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottattTidspunkt;
    }

    public String getKanalreferanse() {
        return kanalreferanse;
    }

    public boolean harPayload() {
        String lob = getPayload();
        return lob != null && !lob.isEmpty();
    }

    public String getPayload() {
        if (payloadString != null && !payloadString.isEmpty()) {
            return payloadString; // quick return, deserialisert tidligere
        }
        if (payload == null || (payloadString != null && payloadString.isEmpty())) {
            return null;  // quick return, har ikke eller er tom
        }

        payloadString = ""; // dummy value for å signalisere at er allerede deserialisert
        try {
            BufferedReader in = new BufferedReader(this.payload.getCharacterStream());
            String line;
            StringBuilder sb = new StringBuilder(2048);
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            payloadString = sb.toString();
        } catch (SQLException | IOException e) {
            throw new PersistenceException("Kunne ikke lese payload: ", e);
        }
        return this.payloadString;
    }

    public void setPayload(String payload) {
        this.payload = payload == null || payload.isEmpty() ? null : ClobProxy.generateProxy(payload);
    }

    void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    void setMottattTidspunkt(LocalDateTime mottattTidspunkt) {
        this.mottattTidspunkt = mottattTidspunkt;
    }

    public void setKanalreferanse(String kanalreferanse) {
        this.kanalreferanse = kanalreferanse;
    }

    public Brevkode getType() {
        return type;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public static class Builder {
        private MottattDokument mottatteDokumentMal;

        public Builder() {
            mottatteDokumentMal = new MottattDokument();
        }

        public Builder medJournalPostId(JournalpostId journalPostId) {
            mottatteDokumentMal.journalpostId = journalPostId;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            mottatteDokumentMal.behandlingId = behandlingId;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            mottatteDokumentMal.mottattDato = mottattDato;
            return this;
        }

        public Builder medMottattTidspunkt(LocalDateTime mottattTidspunkt) {
            mottatteDokumentMal.mottattTidspunkt = mottattTidspunkt;
            return this;
        }

        public Builder medKanalreferanse(String kanalreferanse) {
            mottatteDokumentMal.kanalreferanse = kanalreferanse;
            return this;
        }

        public Builder medPayload(String payload) {
            mottatteDokumentMal.setPayload(payload);
            return this;
        }

        public Builder medFagsakId(Long fagsakId) {
            mottatteDokumentMal.fagsakId = fagsakId;
            return this;
        }

        public Builder medId(Long mottattDokumentId) {
            mottatteDokumentMal.id = mottattDokumentId;
            return this;
        }

        public Builder medType(Brevkode type) {
            mottatteDokumentMal.type = type;
            return this;
        }

        public MottattDokument build() {
            Objects.requireNonNull(mottatteDokumentMal.fagsakId, "Trenger fagsak id for å opprette MottatteDokument.");
            return mottatteDokumentMal;
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof MottattDokument)) {
            return false;
        }
        MottattDokument other = (MottattDokument) obj;
        return Objects.equals(this.type, other.type)
            && Objects.equals(this.journalpostId, other.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, journalpostId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<journalpostId=" + journalpostId
            + ", mottattDato" + mottattDato + "[" + mottattTidspunkt + "]";
    }
}
