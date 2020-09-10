package no.nav.k9.sak.behandling.hendelse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;

public class BehandlingProsessEventDto {
    /**
     * Ekstern id for behandlingen. Id benyttes til oppslag i fagsystem.
     * Benytt samme id for alle oppdateringer av aksjonspunkt/prosess innenfor samme behandling.
     */
    private UUID eksternId;
    private Fagsystem fagsystem;
    private LocalDate behandlingstidFrist;
    private String saksnummer;
    private String aktørId;

    private Long behandlingId; // fjernes etter overgang til eksternId

    /**
     * Tidspunkt for hendelse lokalt for fagsystem.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime eventTid;
    private EventHendelse eventHendelse;
    private String behandlinStatus; // fjernes etter overgang til behandlingStatus
    private String behandlingStatus;
    private String behandlingSteg;
    private String behandlendeEnhet;
    private String ansvarligBeslutterForTotrinn;
    private String ansvarligSaksbehandlerForTotrinn;
    /**
     * @see BehandlingResultatType
     */
    private String resultatType;

    /**
     * Ytelsestype i kodeform. Eksempel: FP
     */
    private String ytelseTypeKode;

    /**
     * Behandlingstype i kodeform. Eksempel: BT-002
     */
    private String behandlingTypeKode;

    /**
     * Tidspunkt behandling ble opprettet
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime opprettetBehandling;

    /**
     * Map av aksjonspunktkode og statuskode.
     */
    private Map<String, String> aksjonspunktKoderMedStatusListe;

    public BehandlingProsessEventDto() {
    }

    protected BehandlingProsessEventDto(Builder builder) {
        this.eksternId = builder.eksternId;
        this.fagsystem = builder.fagsystem;
        this.saksnummer = builder.saksnummer;
        this.aktørId = builder.aktørId;
        this.behandlingId = builder.behandlingId;
        this.eventTid = builder.eventTid;
        this.fagsystem = builder.fagsystem;
        this.behandlingstidFrist = builder.behandlingstidFrist;
        this.eventHendelse = builder.eventHendelse;
        this.behandlinStatus = builder.behandlingStatus;
        this.behandlingStatus = builder.behandlingStatus;
        this.behandlingSteg = builder.behandlingSteg;
        this.behandlendeEnhet = builder.behandlendeEnhet;
        this.ytelseTypeKode = builder.ytelseTypeKode;
        this.resultatType = builder.resultatType;
        this.behandlingTypeKode = builder.behandlingTypeKode;
        this.opprettetBehandling = builder.opprettetBehandling;
        this.aksjonspunktKoderMedStatusListe = builder.aksjonspunktKoderMedStatusListe;
        this.ansvarligSaksbehandlerForTotrinn = builder.ansvarligSaksbehandlerForTotrinn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getEksternId() {
        return eksternId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Fagsystem getFagsystem() {
        return fagsystem;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getAktørId() {
        return aktørId;
    }

    public LocalDateTime getEventTid() {
        return eventTid;
    }

    public EventHendelse getEventHendelse() {
        return eventHendelse;
    }

    public String getBehandlinStatus() {
        return behandlinStatus;
    }

    public String getBehandlingStatus() {
        return behandlingStatus;
    }

    public String getBehandlingSteg() {
        return behandlingSteg;
    }

    public String getBehandlendeEnhet() {
        return behandlendeEnhet;
    }

    public String getYtelseTypeKode() {
        return ytelseTypeKode;
    }

    public String getBehandlingTypeKode() {
        return behandlingTypeKode;
    }

    public LocalDateTime getOpprettetBehandling() {
        return opprettetBehandling;
    }

    public Map<String, String> getAksjonspunktKoderMedStatusListe() {
        return aksjonspunktKoderMedStatusListe;
    }

    public String getAnsvarligBeslutterForTotrinn() {
        return ansvarligBeslutterForTotrinn;
    }

    public String getAnsvarligSaksbehandlerForTotrinn() {
        return ansvarligSaksbehandlerForTotrinn;
    }

    public LocalDate getBehandlingstidFrist() {
        return behandlingstidFrist;
    }
    
    public String getResultatType() {
        return resultatType;
    }

    public static class Builder {
        private UUID eksternId;
        private Fagsystem fagsystem;
        private String saksnummer;
        private String aktørId;
        private LocalDate behandlingstidFrist;
        private Long behandlingId;
        private LocalDateTime eventTid;
        private EventHendelse eventHendelse;
        private String behandlingStatus;
        private String behandlingSteg;
        private String behandlendeEnhet;
        private String ytelseTypeKode;
        private String behandlingTypeKode;
        private LocalDateTime opprettetBehandling;
        private Map<String, String> aksjonspunktKoderMedStatusListe;
        private String resultatType;
        private String ansvarligSaksbehandlerForTotrinn;
        
        
        private Builder() {
            
        }
        

        public Builder medEksternId(UUID eksternId) {
            this.eksternId = eksternId;
            return this;
        }


        public Builder medFagsystem(Fagsystem fagsystem) {
            this.fagsystem = fagsystem;
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder medAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder getBehandlingstidFrist(LocalDate behandlingstidFrist) {
            this.behandlingstidFrist = behandlingstidFrist;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Builder medEventTid(LocalDateTime eventTid) {
            this.eventTid = eventTid;
            return this;
        }

        public Builder medEventHendelse(EventHendelse eventHendelse) {
            this.eventHendelse = eventHendelse;
            return this;
        }

        public Builder medBehandlingStatus(String behandlingStatus) {
            this.behandlingStatus = behandlingStatus;
            return this;
        }

        public Builder medBehandlingSteg(String behandlingSteg) {
            this.behandlingSteg = behandlingSteg;
            return this;
        }

        public Builder medBehandlendeEnhet(String behandlendeEnhet) {
            this.behandlendeEnhet = behandlendeEnhet;
            return this;
        }

        public Builder medYtelseTypeKode(String ytelseTypeKode) {
            this.ytelseTypeKode = ytelseTypeKode;
            return this;
        }

        public Builder medBehandlingTypeKode(String behandlingTypeKode) {
            this.behandlingTypeKode = behandlingTypeKode;
            return this;
        }

        public Builder medOpprettetBehandling(LocalDateTime opprettetBehandling) {
            this.opprettetBehandling = opprettetBehandling;
            return this;
        }

        public Builder medBehandlingResultat(BehandlingResultatType resultatType) {
            Objects.requireNonNull(resultatType);
            this.resultatType = Objects.requireNonNull(resultatType).getKode();
            return this;
        }

        public Builder medAksjonspunktKoderMedStatusListe(Map<String, String> aksjonspunktKoderMedStatusListe) {
            this.aksjonspunktKoderMedStatusListe = aksjonspunktKoderMedStatusListe;
            return this;
        }
        
        public Builder medAnsvarligSaksbehandlerForTotrinn(String ansvarligSaksbehandlerForTotrinn) {
            this.ansvarligSaksbehandlerForTotrinn = ansvarligSaksbehandlerForTotrinn;
            return this;
        }

        public BehandlingProsessEventDto build() {
            return new BehandlingProsessEventDto(this);
        }
    }
}
