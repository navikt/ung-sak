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
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.kontrakt.ResourceLink;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingDto {

    @JsonProperty(value = "ansvarligSaksbehandler")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ansvarligSaksbehandler;

    @JsonProperty(value = "avsluttet")
    private LocalDateTime avsluttet;

    @JsonProperty(value = "behandlendeEnhetId")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String behandlendeEnhetId;

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
    @JsonProperty(value = "behandlingsfristTid")
    private LocalDate behandlingsfristTid;

    @JsonProperty(value = "behandlingsresultat")
    @Valid
    private BehandlingsresultatDto behandlingsresultat;

    @JsonProperty(value = "endret")
    private LocalDateTime endret;

    @JsonProperty(value = "endretAvBrukernavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String endretAvBrukernavn;

    @JsonProperty(value = "erPaaVent")
    private boolean erPaaVent;

    @JsonProperty(value = "fagsakId")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long fagsakId;

    @JsonProperty(value = "førsteÅrsak")
    @Valid
    private BehandlingÅrsakDto førsteÅrsak;

    @JsonAlias("fristBehandlingPåVent")
    @JsonProperty("fristBehandlingPaaVent")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fristBehandlingPåVent;

    @JsonProperty(value = "gjeldendeVedtak")
    private boolean gjeldendeVedtak;

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

    @JsonProperty(value = "originalVedtaksDato")
    private LocalDate originalVedtaksDato;

    @JsonProperty("sprakkode")
    @Valid
    private Språkkode språkkode;

    private BehandlingStatus status;

    @JsonProperty(value = "toTrinnsBehandling")
    private boolean toTrinnsBehandling;

    private BehandlingType type;

    @JsonProperty(value = "uuid", required = true)
    @NotNull
    private UUID uuid;

    @JsonAlias("venteÅrsakKode")
    @JsonProperty("venteArsakKode")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String venteÅrsakKode;

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

    public void setBehandlingsresultat(BehandlingsresultatDto behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
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

    public void setVenteÅrsakKode(String venteÅrsakKode) {
        this.venteÅrsakKode = venteÅrsakKode;
    }

    public void setVersjon(Long versjon) {
        this.versjon = versjon;
    }

    void setBehandlingKøet(boolean behandlingKøet) {
        this.behandlingKøet = behandlingKøet;
    }
}
