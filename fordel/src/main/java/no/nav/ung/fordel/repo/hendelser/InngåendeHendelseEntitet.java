package no.nav.ung.fordel.repo.hendelser;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.hendelser.HendelseType;

@Entity(name = "InngåendeHendelse")
@Table(name = "FORDEL_INNGAAENDE_HENDELSE")
public class InngåendeHendelseEntitet {
    public enum HåndtertStatusType {
        MOTTATT("MOTTATT"),
        HÅNDTERT("HÅNDTERT");

        private final String dbKode;

        HåndtertStatusType(String dbKode) {
            this.dbKode = dbKode;
        }

        public String getDbKode() {
            return dbKode;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FORDEL_INNGAAENDE_HENDELSE")
    @Column(name = "id")
    private Long id;

    @Column(name = "hendelse_id")
    private String hendelseId;

    @Column(name = "aktoer_id")
    private String aktørId;

    @Column(name="type", nullable = false)
    private String hendelseType;

    @Column(name = "payload")
    private String payload;

    @Column(name = "melding_opprettet")
    private LocalDateTime melding_opprettet;

    @Column(name="haandtert_status", nullable = false)
    private String håndtertStatus;

    @Column(name = "haandtert_av_hendelse_id")
    private String håndtertAvHendelseId;

    @Column(name = "opprettet_tid", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    @PrePersist
    protected void onCreate() {
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : LocalDateTime.now();
    }

    InngåendeHendelseEntitet() {
        // Hibernate
    }

    private InngåendeHendelseEntitet(Builder builder) {
        this.id = builder.id;
        this.hendelseId = builder.hendelseId;
        this.aktørId = builder.aktørId;
        this.hendelseType = builder.hendelseType;
        this.payload = builder.payload;
        this.melding_opprettet = builder.melding_opprettet;
        this.håndtertStatus = builder.håndtertStatus;
        this.håndtertAvHendelseId = builder.håndtertAvHendelseId;
    }

    public Long getId() {
        return id;
    }

    public String getHendelseId() {
        return hendelseId;
    }

    public String getAktørId() {
        return aktørId;
    }

    public String getPayload() {
        return payload;
    }

    public HåndtertStatusType getHåndtertStatus() {
        return HåndtertStatusType.valueOf(this.håndtertStatus);
    }

    public HendelseType getHendelseType() {
        return HendelseType.fraKode(this.hendelseType);
    }

    public void setHåndtertStatus(HåndtertStatusType håndtertStatus) {
        this.håndtertStatus = håndtertStatus.getDbKode();
    }

    public void setHåndtertAvHendelseId(String håndtertAvHendelseId) {
        this.håndtertAvHendelseId = håndtertAvHendelseId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public String håndtertAvHendelseId;
        private Long id;
        private String hendelseId;
        private String aktørId;
        private String hendelseType;
        private String payload;
        private LocalDateTime melding_opprettet;
        private String håndtertStatus;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
            return this;
        }

        public Builder aktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder hendelseType(HendelseType hendelseType) {
            this.hendelseType = hendelseType.getKode();
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder meldingOpprettet(LocalDateTime melding_opprettet) {
            this.melding_opprettet = melding_opprettet;
            return this;
        }

        public Builder håndtertStatus(HåndtertStatusType håndtertStatus) {
            this.håndtertStatus = håndtertStatus.getDbKode();
            return this;
        }

        public InngåendeHendelseEntitet build() {
            return new InngåendeHendelseEntitet(this);
        }
    }
}
