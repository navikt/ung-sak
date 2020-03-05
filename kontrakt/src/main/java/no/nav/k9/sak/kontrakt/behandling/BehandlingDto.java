package no.nav.k9.sak.kontrakt.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.kontrakt.AsyncPollingStatus;
import no.nav.k9.sak.kontrakt.ResourceLink;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingDto {

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "ansvarligSaksbehandler")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ansvarligSaksbehandler;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "avsluttet")
    private LocalDateTime avsluttet;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "behandlendeEnhetId")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String behandlendeEnhetId;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "behandlendeEnhetNavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String behandlendeEnhetNavn;

    @JsonProperty(value = "behandlingÅrsaker")
    @Size(max = 20)
    @Valid
    private List<BehandlingÅrsakDto> behandlingÅrsaker = new ArrayList<>();

    @JsonProperty("behandlingKoet")
    private boolean behandlingKøet;

    @JsonProperty("behandlingPaaVent")
    private boolean behandlingPåVent;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "behandlingsfristTid")
    private LocalDate behandlingsfristTid;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "behandlingsresultat")
    @Valid
    private BehandlingsresultatDto behandlingsresultat;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "behandlingResultatType")
    @Valid
    private BehandlingResultatType behandlingResultatType;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "endret")
    private LocalDateTime endret;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "endretAvBrukernavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String endretAvBrukernavn;

    @JsonProperty(value = "erPaaVent")
    private boolean erPaaVent;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "fagsakId")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long fagsakId;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "førsteÅrsak")
    @Valid
    private BehandlingÅrsakDto førsteÅrsak;

    @JsonInclude(value = Include.NON_NULL)
    @JsonAlias("fristBehandlingPåVent")
    @JsonProperty("fristBehandlingPaaVent")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fristBehandlingPåVent;

    @JsonProperty(value = "gjeldendeVedtak")
    private boolean gjeldendeVedtak;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "id")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long id;

    /**
     * REST HATEOAS - pekere på data innhold som hentes fra andre url'er, eller handlinger som er tilgjengelig på behandling.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    @JsonProperty(value = "opprettet", required = true)
    @NotNull
    private LocalDateTime opprettet;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "originalVedtaksDato")
    private LocalDate originalVedtaksDato;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty("sprakkode")
    @Valid
    private Språkkode språkkode;

    @JsonProperty(value = "status", required = true)
    @Valid
    private BehandlingStatus status;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "stegTilstand")
    @Valid
    private BehandlingStegTilstandDto stegTilstand;

    @JsonProperty(value = "toTrinnsBehandling")
    private boolean toTrinnsBehandling;

    @JsonProperty(value = "type", required = true)
    @Valid
    @NotNull
    private BehandlingType type;

    @JsonProperty(value = "uuid", required = true)
    @NotNull
    private UUID uuid;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty("venteårsak")
    @Valid
    private Venteårsak venteårsak;

    /** @deprecated bruk #venteÅrsak */
    @Deprecated(forRemoval = true)
    @JsonInclude(value = Include.NON_NULL)
    @JsonAlias("venteÅrsakKode")
    @JsonProperty("venteArsakKode")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String venteÅrsakKode;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty("ansvarligBeslutter")
    @Size(max = 100000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ansvarligBeslutter;

    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty(value = "behandlingHenlagt")
    private Boolean behandlingHenlagt;

    /** Eventuelt async status på tasks. */
    @JsonInclude(value = Include.NON_NULL)
    @JsonProperty("taskStatus")
    @Valid
    private AsyncPollingStatus taskStatus;

    @JsonProperty(value = "versjon", required = true)
    @NotNull
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long versjon;

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public LocalDateTime getAvsluttet() {
        return avsluttet;
    }

    public String getBehandlendeEnhetId() {
        return behandlendeEnhetId;
    }

    public String getBehandlendeEnhetNavn() {
        return behandlendeEnhetNavn;
    }

    public List<BehandlingÅrsakDto> getBehandlingÅrsaker() {
        return Collections.unmodifiableList(behandlingÅrsaker);
    }

    public LocalDate getBehandlingsfristTid() {
        return behandlingsfristTid;
    }

    public BehandlingsresultatDto getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public LocalDateTime getEndret() {
        return endret;
    }

    public String getEndretAvBrukernavn() {
        return endretAvBrukernavn;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public BehandlingÅrsakDto getFørsteÅrsak() {
        return førsteÅrsak;
    }

    public String getFristBehandlingPåVent() {
        return fristBehandlingPåVent;
    }

    public Long getId() {
        return id;
    }

    public List<ResourceLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public LocalDate getOriginalVedtaksDato() {
        return originalVedtaksDato;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public BehandlingType getType() {
        return type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getVenteÅrsakKode() {
        return venteÅrsakKode;
    }

    public Long getVersjon() {
        return versjon;
    }

    public boolean isBehandlingKøet() {
        return behandlingKøet;
    }

    public boolean isBehandlingPåVent() {
        return behandlingPåVent;
    }

    public boolean isErPaaVent() {
        return erPaaVent;
    }

    public boolean isGjeldendeVedtak() {
        return gjeldendeVedtak;
    }

    public void leggTil(ResourceLink link) {
        links.add(link);
    }

    public void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public void setAvsluttet(LocalDateTime avsluttet) {
        this.avsluttet = avsluttet;
    }

    public void setBehandlendeEnhetId(String behandlendeEnhetId) {
        this.behandlendeEnhetId = behandlendeEnhetId;
    }

    public void setBehandlendeEnhetNavn(String behandlendeEnhetNavn) {
        this.behandlendeEnhetNavn = behandlendeEnhetNavn;
    }

    public void setBehandlingArsaker(List<BehandlingÅrsakDto> behandlingÅrsaker) {
        this.behandlingÅrsaker = List.copyOf(behandlingÅrsaker);
    }

    public void setBehandlingÅrsaker(List<BehandlingÅrsakDto> behandlingÅrsaker) {
        this.behandlingÅrsaker = behandlingÅrsaker;
    }

    public void setBehandlingPåVent(boolean behandlingPåVent) {
        this.behandlingPåVent = behandlingPåVent;
    }

    public void setBehandlingsfristTid(LocalDate behandlingsfristTid) {
        this.behandlingsfristTid = behandlingsfristTid;
    }

    public void setBehandlingsresultat(BehandlingsresultatDto dto) {
        this.behandlingsresultat = dto;
    }

    public void setBehandlingStegTilstand(BehandlingStegTilstandDto stegTilstand) {
        this.stegTilstand = stegTilstand;
    }

    public void setEndret(LocalDateTime endret) {
        this.endret = endret;
    }

    public void setEndretAvBrukernavn(String endretAvBrukernavn) {
        this.endretAvBrukernavn = endretAvBrukernavn;
    }

    public void setErPaaVent(boolean erPaaVent) {
        this.erPaaVent = erPaaVent;
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public void setFørsteÅrsak(BehandlingÅrsakDto førsteÅrsak) {
        this.førsteÅrsak = førsteÅrsak;
    }

    public void setFristBehandlingPåVent(String fristBehandlingPåVent) {
        this.fristBehandlingPåVent = fristBehandlingPåVent;
    }

    public void setGjeldendeVedtak(boolean gjeldendeVedtak) {
        this.gjeldendeVedtak = gjeldendeVedtak;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLinks(List<ResourceLink> links) {
        this.links = links;
    }

    public void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    public void setOriginalVedtaksDato(LocalDate originalVedtaksDato) {
        this.originalVedtaksDato = originalVedtaksDato;
    }

    public void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public void setType(BehandlingType type) {
        this.type = type;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setVenteårsak(Venteårsak venteårsak) {
        this.venteårsak = venteårsak;
        this.venteÅrsakKode = venteårsak == null ? null : venteårsak.getKode();
    }

    public void setVersjon(Long versjon) {
        this.versjon = versjon;
    }

    public void setBehandlingKøet(boolean behandlingKøet) {
        this.behandlingKøet = behandlingKøet;
    }
    
    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public AsyncPollingStatus getTaskStatus() {
        return taskStatus;
    }

    public boolean isBehandlingHenlagt() {
        return behandlingHenlagt;
    }

    public void setAnsvarligBeslutter(String ansvarligBeslutter) {
        this.ansvarligBeslutter = ansvarligBeslutter;
    }

    public void setAsyncStatus(AsyncPollingStatus asyncStatus) {
        this.taskStatus = asyncStatus;
    }

    public void setBehandlingHenlagt(boolean behandlingHenlagt) {
        this.behandlingHenlagt = behandlingHenlagt;
    }

    public void setTaskStatus(AsyncPollingStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
}
