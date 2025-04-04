package no.nav.ung.sak.kontrakt.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.hendelse.EventHendelse;
import no.nav.ung.sak.kontrakt.aksjonspunkt.AksjonspunktTilstandDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingProsessHendelse {

    /**
     * Ekstern id for behandlingen (behandlingUuid). Id benyttes til oppslag i fagsystem.
     * Benytt samme id for alle oppdateringer av aksjonspunkt/prosess innenfor samme behandling.
     */
    @NotNull
    @Valid
    @JsonProperty(value = "eksternId", required = true)
    private UUID eksternId;

    @NotNull
    @Valid
    @JsonProperty(value = "fagsystem", required = true)
    private Fagsystem fagsystem;

    /**
     * Eventuell frist for behandlingen.
     */
    @Valid
    @JsonProperty(value = "behandlingstidFrist", required = false)
    private LocalDate behandlingstidFrist;

    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    @JsonProperty(value = "saksnummer", required = true)
    private String saksnummer;

    /**
     * Brukers aktørId.
     */
    @NotNull
    @Valid
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "aktørId", required = true)
    private String aktørId;

    /**
     * Tidspunkt for hendelse lokalt for fagsystem.
     */
    @NotNull
    @Valid
    @JsonProperty(value = "eventTid", required = true)
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime eventTid;

    /**
     * Beskrivelse av hendelse ({@link EventHendelse}).
     */
    @NotNull
    @Valid
    @JsonProperty(value = "eventHendelse", required = true)
    private EventHendelse eventHendelse;

    /**
     * Nåværende {@link BehandlingStatus} for behandling.
     */
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    @JsonProperty(value = "behandlingStatus", required = true)
    private String behandlingStatus;

    /**
     * Hvilket {@link BehandlingStegType} behandlingen står i p.t..
     */
    @Valid
    @JsonProperty(value = "behandlingSteg", required = false)
    private String behandlingSteg;

    @Valid
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "behandlendeEnhet", required = false)
    private String behandlendeEnhet;

    /**
     * Ident for saksbehandler ansvarlig for totrinn (beslutter rolle).
     */
    @Valid
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "ansvarligBeslutterForTotrinn", required = false)
    private String ansvarligBeslutterForTotrinn;

    /**
     * Ident for saksbehandler ansvarlig for totrinn (saksbehandler rolle).
     */
    @Valid
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "ansvarligSaksbehandlerForTotrinn", required = false)
    private String ansvarligSaksbehandlerForTotrinn;

    /**
     * @see BehandlingResultatType
     */
    @Valid
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "resultatType", required = false)
    private String resultatType;

    /**
     * Ytelsestype i kodeform. Eksempel: FP
     */
    @NotNull
    @Valid
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "ytelseTypeKode", required = true)
    private String ytelseTypeKode;

    /**
     * Behandlingstype i kodeform. Eksempel: BT-002
     */
    @NotNull
    @Valid
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}\\-_.]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @JsonProperty(value = "behandlingTypeKode", required = true)
    private String behandlingTypeKode;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Valid
    @JsonProperty(value = "eldsteDatoMedEndringFraSøker", required = true)
    private LocalDateTime eldsteDatoMedEndringFraSøker;

    /**
     * Tidspunkt behandling ble opprettet
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @NotNull
    @Valid
    @JsonProperty(value = "opprettetBehandling", required = true)
    private LocalDateTime opprettetBehandling;

    /**
     * Map av aksjonspunktkode og statuskode.
     */
    @JsonInclude(value = Include.ALWAYS)
    @Valid
    @JsonProperty(value = "aksjonspunktKoderMedStatusListe", required = true)
    private Map<String, String> aksjonspunktKoderMedStatusListe;

    /**
     * @NotNull produseres alltid, men er nullable for kompatiblitet med eldre versjoner
     */
    @Valid
    @JsonProperty(value = "fagsakPeriode", required = false)
    private Periode fagsakPeriode;

    @Valid
    @JsonProperty(value = "aksjonspunktTilstander", required = true)
    private List<AksjonspunktTilstandDto> aksjonspunktTilstand;

    /**
     * {@code true} hvis det finnes minst én ny periode det er søkt om i behandlingen.
     */
    @Valid
    @JsonProperty(value = "nyeKrav", required = false)
    private Boolean nyeKrav;

    @Valid
    @JsonProperty(value = "fraEndringsdialog", required = false)
    private Boolean fraEndringsdialog;

    @Valid
    @JsonProperty(value = "vedtaksdato", required = false)
    private LocalDate vedtaksdato;

    @Valid
    @JsonProperty(value = "behandlingsårsaker")
    private List<String> behandlingsårsaker;

    @Valid
    @JsonProperty(value = "søknadsårsaker")
    private List<String> søknadsårsaker;

    public BehandlingProsessHendelse() {
    }

    protected BehandlingProsessHendelse(BehandlingProsessHendelse kopierFra) {
        this.eksternId = kopierFra.eksternId;
        this.fagsystem = kopierFra.fagsystem;
        this.saksnummer = kopierFra.saksnummer;
        this.aktørId = kopierFra.aktørId;
        this.eventTid = kopierFra.eventTid;
        this.behandlingstidFrist = kopierFra.behandlingstidFrist;
        this.eventHendelse = kopierFra.eventHendelse;
        this.behandlingStatus = kopierFra.behandlingStatus;
        this.behandlingSteg = kopierFra.behandlingSteg;
        this.behandlendeEnhet = kopierFra.behandlendeEnhet;
        this.ytelseTypeKode = kopierFra.ytelseTypeKode;
        this.resultatType = kopierFra.resultatType;
        this.behandlingTypeKode = kopierFra.behandlingTypeKode;
        this.opprettetBehandling = kopierFra.opprettetBehandling;
        this.eldsteDatoMedEndringFraSøker = kopierFra.eldsteDatoMedEndringFraSøker;
        this.aksjonspunktKoderMedStatusListe = kopierFra.aksjonspunktKoderMedStatusListe;
        this.ansvarligSaksbehandlerForTotrinn = kopierFra.ansvarligSaksbehandlerForTotrinn;
        this.fagsakPeriode = kopierFra.fagsakPeriode;
        this.ansvarligBeslutterForTotrinn = kopierFra.ansvarligBeslutterForTotrinn;
        this.aksjonspunktTilstand = kopierFra.aksjonspunktTilstand.stream().map(AksjonspunktTilstandDto::new).toList();
        this.nyeKrav = kopierFra.nyeKrav;
        this.vedtaksdato = kopierFra.vedtaksdato;
        this.fraEndringsdialog = kopierFra.fraEndringsdialog;
        this.behandlingsårsaker = kopierFra.behandlingsårsaker;
        this.søknadsårsaker = kopierFra.søknadsårsaker;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getEksternId() {
        return eksternId;
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

    public List<AksjonspunktTilstandDto> getAksjonspunktTilstand() {
        return aksjonspunktTilstand;
    }

    public boolean isNyeKrav() {
        return nyeKrav != null && nyeKrav;
    }

    public boolean isFraEndringsdialog() {
        return fraEndringsdialog != null && fraEndringsdialog;
    }

    public List<String> getBehandlingsårsaker() {
        return behandlingsårsaker;
    }

    public List<String> getSøknadsårsaker() {
        return søknadsårsaker;
    }

    public LocalDate getVedtaksdato() {
        return vedtaksdato;
    }

    public static class Builder {
        private BehandlingProsessHendelse kladd = new BehandlingProsessHendelse();

        private Builder() {
        }

        public Builder medEksternId(UUID eksternId) {
            kladd.eksternId = eksternId;
            return this;
        }

        public Builder medFagsystem(Fagsystem fagsystem) {
            kladd.fagsystem = fagsystem;
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medAktørId(String aktørId) {
            kladd.aktørId = aktørId;
            return this;
        }

        public Builder getBehandlingstidFrist(LocalDate behandlingstidFrist) {
            kladd.behandlingstidFrist = behandlingstidFrist;
            return this;
        }

        public Builder medEventTid(LocalDateTime eventTid) {
            kladd.eventTid = eventTid;
            return this;
        }

        public Builder medEventHendelse(EventHendelse eventHendelse) {
            kladd.eventHendelse = eventHendelse;
            return this;
        }

        public Builder medBehandlingStatus(String behandlingStatus) {
            kladd.behandlingStatus = behandlingStatus;
            return this;
        }

        public Builder medBehandlingSteg(String behandlingSteg) {
            kladd.behandlingSteg = behandlingSteg;
            return this;
        }

        public Builder medBehandlendeEnhet(String behandlendeEnhet) {
            kladd.behandlendeEnhet = behandlendeEnhet;
            return this;
        }

        public Builder medAnsvarligBeslutterForTotrinn(String ansvarligBeslutterForTotrinn) {
            kladd.ansvarligBeslutterForTotrinn = ansvarligBeslutterForTotrinn;
            return this;
        }

        public Builder medYtelseTypeKode(String ytelseTypeKode) {
            kladd.ytelseTypeKode = ytelseTypeKode;
            return this;
        }

        public Builder medBehandlingTypeKode(String behandlingTypeKode) {
            kladd.behandlingTypeKode = behandlingTypeKode;
            return this;
        }

        public Builder medOpprettetBehandling(LocalDateTime opprettetBehandling) {
            kladd.opprettetBehandling = opprettetBehandling;
            return this;
        }

        public Builder medEldsteDatoMedEndringFraSøker(LocalDateTime eldsteDatoMedEndringFraSøker) {
            kladd.eldsteDatoMedEndringFraSøker = eldsteDatoMedEndringFraSøker;
            return this;
        }

        public Builder medBehandlingResultat(BehandlingResultatType resultatType) {
            Objects.requireNonNull(resultatType);
            kladd.resultatType = Objects.requireNonNull(resultatType).getKode();
            return this;
        }

        public Builder medAksjonspunktKoderMedStatusListe(Map<String, String> aksjonspunktKoderMedStatusListe) {
            kladd.aksjonspunktKoderMedStatusListe = aksjonspunktKoderMedStatusListe;
            return this;
        }

        public Builder medAnsvarligSaksbehandlerForTotrinn(String ansvarligSaksbehandlerForTotrinn) {
            kladd.ansvarligSaksbehandlerForTotrinn = ansvarligSaksbehandlerForTotrinn;
            return this;
        }

        public Builder medFagsakPeriode(Periode periode) {
            kladd.fagsakPeriode = periode;
            return this;
        }

        public Builder medAksjonspunktTilstander(List<AksjonspunktTilstandDto> aksjonspunktTilstand) {
            kladd.aksjonspunktTilstand = aksjonspunktTilstand;
            return this;
        }

        public Builder medNyeKrav(Boolean nyeKrav) {
            kladd.nyeKrav = nyeKrav;
            return this;
        }

        public Builder medVedtaksdato(LocalDate vedtaksdato) {
            kladd.vedtaksdato = vedtaksdato;
            return this;
        }

        public Builder medFraEndringsdialog(Boolean fraEndringsdialog) {
            kladd.fraEndringsdialog = fraEndringsdialog;
            return this;
        }

        public Builder medBehandlingsårsaker(List<String> behandlingsårsaker) {
            kladd.behandlingsårsaker = behandlingsårsaker;
            return this;
        }
        public Builder medSøknadårsaker(List<String> søknadsårsaker) {
            kladd.søknadsårsaker = søknadsårsaker;
            return this;
        }

        public BehandlingProsessHendelse build() {
            return new BehandlingProsessHendelse(this.kladd); // lager en kopi, så kan denne builderen gjenbrukes
        }
    }
}
