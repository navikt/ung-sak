package no.nav.k9.sak.hendelsemottak.k9fordel.domene;

import java.time.LocalDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.k9.kodeverk.hendelser.HendelseKilde;
import no.nav.k9.kodeverk.hendelser.HendelseType;
import no.nav.k9.kodeverk.hendelser.HåndtertStatusType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.HendelseKildeKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.HendelseTypeKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.HåndtertStatusTypeKodeverdiConverter;
import no.nav.k9.sak.typer.AktørId;

@Entity(name = "InngåendeHendelse")
@Table(name = "INNGAAENDE_HENDELSE")
public class InngåendeHendelse extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNGAAENDE_HENDELSE")
    @Column(name = "id")
    private Long id;

    @Column(name = "hendelse_id")
    private String hendelseId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Convert(converter = HendelseTypeKodeverdiConverter.class)
    @Column(name="type", nullable = false)
    private HendelseType hendelseType;

    @Convert(converter = HendelseKildeKodeverdiConverter.class)
    @Column(name="kilde", nullable = false)
    private HendelseKilde hendelseKilde;

    @Column(name = "payload")
    private String payload;

    @Column(name = "melding_opprettet")
    private LocalDateTime melding_opprettet;

    @Convert(converter = HåndtertStatusTypeKodeverdiConverter.class)
    @Column(name="haandtert_status", nullable = false)
    private HåndtertStatusType håndtertStatus = HåndtertStatusType.MOTTATT;

    @Column(name = "haandtert_av_hendelse_id")
    private String håndtertAvHendelseId;

    InngåendeHendelse() {
        // Hibernate
    }

    private InngåendeHendelse(Builder builder) {
        this.id = builder.id;
        this.hendelseId = builder.hendelseId;
        this.aktørId = builder.aktørId;
        this.hendelseKilde = builder.hendelseKilde;
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

    public AktørId getAktørId() {
        return aktørId;
    }

    public String getPayload() {
        return payload;
    }

    public HåndtertStatusType getHåndtertStatus() {
        return håndtertStatus;
    }

    public HendelseType getHendelseType() {
        return hendelseType;
    }

    public HendelseKilde getHendelseKilde() {
        return hendelseKilde;
    }

    void setHåndtertStatus(HåndtertStatusType håndtertStatus) {
        this.håndtertStatus = håndtertStatus;
    }

    void setHåndtertAvHendelseId(String håndtertAvHendelseId) {
        this.håndtertAvHendelseId = håndtertAvHendelseId;
    }

    public static Builder builder() {
        return new Builder();
    }



    public static class Builder {
        public String håndtertAvHendelseId;
        private Long id;
        private String hendelseId;
        private AktørId aktørId;
        private HendelseKilde hendelseKilde;
        private HendelseType hendelseType;
        private String payload;
        private LocalDateTime melding_opprettet;
        private HåndtertStatusType håndtertStatus;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder hendelseId(String hendelseId) {
            this.hendelseId = hendelseId;
            return this;
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder hendelseKilde(HendelseKilde hendelseKilde) {
            this.hendelseKilde = hendelseKilde;
            return this;
        }

        public Builder hendelseType(HendelseType hendelseType) {
            this.hendelseType = hendelseType;
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
            this.håndtertStatus = håndtertStatus;
            return this;
        }

        public InngåendeHendelse build() {
            return new InngåendeHendelse(this);
        }
    }
}
