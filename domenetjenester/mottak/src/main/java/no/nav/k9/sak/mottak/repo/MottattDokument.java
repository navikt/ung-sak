package no.nav.k9.sak.mottak.repo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.kodeverk.dokument.DokumentGruppe;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.DokumentKategoriKodeverdiConverter;
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

    @Column(name = "forsendelse_id")
    private UUID forsendelseId;

    @Column(name = "journal_enhet")
    private String journalEnhet;

    @Convert(converter = DokumentKategoriKodeverdiConverter.class)
    @Column(name = "dokument_kategori", nullable = false, updatable = false)
    private DokumentKategori dokumentKategori = DokumentKategori.UDEFINERT;

    @Column(name = "behandling_id", updatable = false)
    private Long behandlingId;

    @Column(name = "mottatt_dato", updatable = false)
    private LocalDate mottattDato;

    @Column(name = "mottatt_tidspunkt", updatable = false)
    private LocalDateTime mottattTidspunkt;

    @Column(name = "kanalreferanse", updatable = false)
    private String kanalreferanse;

    @Column(name = "type", updatable = false)
    private String dokumentTypeId;

    @Lob
    @Column(name = "payload", updatable = false)
    private String payload;

    @Column(name = "payload_type", updatable = false)
    private String payloadType;

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

    public DokumentKategori getDokumentKategori() {
        return dokumentKategori;
    }

    public Optional<String> getJournalEnhet() {
        return Optional.ofNullable(journalEnhet);
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

    public String getPayload() {
        return payload;
    }

    /** @return "XML", "JSON", null. */
    public String getPayloadType() {
        return this.payloadType;
    }

    void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    void setDokumentKategori(DokumentKategori dokumentKategori) {
        this.dokumentKategori = dokumentKategori;
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

    void setPayload(String payload) {
        this.payload = payload;
        if (payload != null && !payload.isEmpty()) {
            switch (payload.charAt(0)) {
                case '<':
                    this.payloadType = "XML";
                    break;
                case '{':
                    this.payloadType = "JSON";
                    break;
                default:
                    this.payloadType = null;
            }
        }
    }

    void setDokumentType(String dokumentIdType) {
        this.dokumentTypeId = dokumentIdType;
    }

    public void setKanalreferanse(String kanalreferanse) {
        this.kanalreferanse = kanalreferanse;
    }

    public String getDokumentTypeId() {
        return dokumentTypeId;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public UUID getForsendelseId() {
        return forsendelseId;
    }

    public void setForsendelseId(UUID forsendelseId) {
        this.forsendelseId = forsendelseId;
    }

    public void setJournalEnhet(String enhet) {
        this.journalEnhet = enhet;
    }

    public static class Builder {
        private MottattDokument mottatteDokumentMal;

        public Builder() {
            mottatteDokumentMal = new MottattDokument();
        }

        public static Builder ny() {
            return new Builder();
        }

        public Builder medDokumentKategori(DokumentKategori dokumentKategori) {
            mottatteDokumentMal.dokumentKategori = dokumentKategori;
            return this;
        }

        public Builder medJournalPostId(JournalpostId journalPostId) {
            mottatteDokumentMal.journalpostId = journalPostId;
            return this;
        }

        public Builder medJournalFørendeEnhet(String journalEnhet) {
            mottatteDokumentMal.journalEnhet = journalEnhet;
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

        public Builder medForsendelseId(UUID forsendelseId) {
            mottatteDokumentMal.forsendelseId = forsendelseId;
            return this;
        }

        public Builder medId(Long mottattDokumentId) {
            mottatteDokumentMal.id = mottattDokumentId;
            return this;
        }

        public Builder medDokumentTypeId(String dokumentTypeId) {
            mottatteDokumentMal.dokumentTypeId = dokumentTypeId;
            return this;
        }

        public MottattDokument build() {
            Objects.requireNonNull(mottatteDokumentMal.fagsakId, "Trenger fagsak id for å opprette MottatteDokument.");
            return mottatteDokumentMal;
        }

        public Builder medDokumentTypeId(DokumentTypeId dokumentTypeId) {
            return medDokumentTypeId(dokumentTypeId.getOffisiellKode());
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
        return Objects.equals(this.dokumentKategori, other.dokumentKategori)
            && Objects.equals(this.dokumentTypeId, other.dokumentTypeId)
            && Objects.equals(this.journalpostId, other.journalpostId)
            && Objects.equals(this.payload, other.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentKategori, dokumentTypeId, journalpostId, payload);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<journalpostId=" + journalpostId
            + ", mottattDato" + mottattDato + "[" + mottattTidspunkt + "]"
            + ", dokumentKategori=" + dokumentKategori
            + (payload != null ? ", payload=\\n" + payload + "\\n>" : ">");
    }
}
