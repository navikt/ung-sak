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

    @JsonProperty(value = "id")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long id;

    @JsonProperty(value = "uuid", required = true)
    @NotNull
    private UUID uuid;

    @JsonProperty(value = "versjon", required = true)
    @NotNull
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long versjon;

    public boolean isBehandlingKøet() {
        return behandlingKøet;
    }

    public void setBehandlingÅrsaker(List<BehandlingÅrsakDto> behandlingÅrsaker) {
        this.behandlingÅrsaker = behandlingÅrsaker;
    }

    public void setLinks(List<ResourceLink> links) {
        this.links = links;
    }

    private BehandlingType type;
    private BehandlingStatus status;

    @JsonProperty(value = "fagsakId")
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long fagsakId;

    @JsonProperty(value = "opprettet", required = true)
    @NotNull
    private LocalDateTime opprettet;

    @JsonProperty(value = "avsluttet")
    private LocalDateTime avsluttet;

    @JsonProperty(value = "endret")
    private LocalDateTime endret;

    @JsonProperty(value="førsteÅrsak")
    @Valid
    private BehandlingÅrsakDto førsteÅrsak;
    
    @JsonProperty(value="behandlingsfristTid")
    private LocalDate behandlingsfristTid;
    
    @JsonProperty(value="gjeldendeVedtak")
    private boolean gjeldendeVedtak;
    
    @JsonProperty(value="erPaaVent")
    private boolean erPaaVent;
    
    @JsonProperty(value="originalVedtaksDato")
    private LocalDate originalVedtaksDato;

    @JsonProperty("behandlingPaaVent")
    private boolean behandlingPåVent;

    @JsonProperty("sprakkode")
    @Valid
    private Språkkode språkkode;

    @JsonProperty("behandlingKoet")
    private boolean behandlingKøet;

    @JsonProperty(value = "toTrinnsBehandling")
    private boolean toTrinnsBehandling;

    @JsonProperty(value = "behandlingsresultat")
    @Valid
    private BehandlingsresultatDto behandlingsresultat;

    @JsonProperty(value = "behandlingÅrsaker")
    @Size(max = 20)
    @Valid
    private List<BehandlingÅrsakDto> behandlingÅrsaker = new ArrayList<>();

    @JsonAlias("fristBehandlingPåVent")
    @JsonProperty("fristBehandlingPaaVent")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fristBehandlingPåVent;

    @JsonAlias("venteÅrsakKode")
    @JsonProperty("venteArsakKode")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String venteÅrsakKode;

    @JsonProperty(value = "ansvarligSaksbehandler")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String ansvarligSaksbehandler;

    @JsonProperty(value = "endretAvBrukernavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String endretAvBrukernavn;

    @JsonProperty(value = "behandlendeEnhetId")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String behandlendeEnhetId;

    @JsonProperty(value = "behandlendeEnhetNavn")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String behandlendeEnhetNavn;

    /**
     * REST HATEOAS - pekere på data innhold som hentes fra andre url'er, eller handlinger som er tilgjengelig på behandling.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Long getVersjon() {
        return versjon;
    }

    public BehandlingType getType() {
        return type;
    }

    public LocalDateTime getOpprettet() {
        return opprettet;
    }

    public LocalDateTime getAvsluttet() {
        return avsluttet;
    }

    public BehandlingStatus getStatus() {
        return status;
    }

    public LocalDateTime getEndret() {
        return endret;
    }

    public BehandlingsresultatDto getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public String getEndretAvBrukernavn() {
        return endretAvBrukernavn;
    }

    public String getBehandlendeEnhetId() {
        return behandlendeEnhetId;
    }

    public String getBehandlendeEnhetNavn() {
        return behandlendeEnhetNavn;
    }

    public BehandlingÅrsakDto getFørsteÅrsak() {
        return førsteÅrsak;
    }

    public List<ResourceLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public boolean isBehandlingPåVent() {
        return behandlingPåVent;
    }

    public String getFristBehandlingPåVent() {
        return fristBehandlingPåVent;
    }

    public String getVenteÅrsakKode() {
        return venteÅrsakKode;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    public boolean isBehandlingKoet() {
        return behandlingKøet;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public List<BehandlingÅrsakDto> getBehandlingÅrsaker() {
        return Collections.unmodifiableList(behandlingÅrsaker);
    }

    public void setBehandlingArsaker(List<BehandlingÅrsakDto> behandlingÅrsaker) {
        this.behandlingÅrsaker = List.copyOf(behandlingÅrsaker);
    }

    public void setFagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setVersjon(Long versjon) {
        this.versjon = versjon;
    }

    public void setType(BehandlingType type) {
        this.type = type;
    }

    public void setOpprettet(LocalDateTime opprettet) {
        this.opprettet = opprettet;
    }

    public void setEndret(LocalDateTime endret) {
        this.endret = endret;
    }

    public void setEndretAvBrukernavn(String endretAvBrukernavn) {
        this.endretAvBrukernavn = endretAvBrukernavn;
    }

    public void setAvsluttet(LocalDateTime avsluttet) {
        this.avsluttet = avsluttet;
    }

    public void setBehandlingsresultat(BehandlingsresultatDto behandlingsresultat) {
        this.behandlingsresultat = behandlingsresultat;
    }

    public void setStatus(BehandlingStatus status) {
        this.status = status;
    }

    public void setBehandlendeEnhetId(String behandlendeEnhetId) {
        this.behandlendeEnhetId = behandlendeEnhetId;
    }

    public void setBehandlendeEnhetNavn(String behandlendeEnhetNavn) {
        this.behandlendeEnhetNavn = behandlendeEnhetNavn;
    }

    public void setFørsteÅrsak(BehandlingÅrsakDto førsteÅrsak) {
        this.førsteÅrsak = førsteÅrsak;
    }

    public void leggTil(ResourceLink link) {
        links.add(link);
    }

    public LocalDate getBehandlingsfristTid() {
        return behandlingsfristTid;
    }

    public void setBehandlingsfristTid(LocalDate behandlingsfristTid) {
        this.behandlingsfristTid = behandlingsfristTid;
    }

    public boolean isGjeldendeVedtak() {
        return gjeldendeVedtak;
    }

    public void setGjeldendeVedtak(boolean gjeldendeVedtak) {
        this.gjeldendeVedtak = gjeldendeVedtak;
    }

    public boolean isErPaaVent() {
        return erPaaVent;
    }

    public void setErPaaVent(boolean erPaaVent) {
        this.erPaaVent = erPaaVent;
    }

    public LocalDate getOriginalVedtaksDato() {
        return originalVedtaksDato;
    }

    public void setOriginalVedtaksDato(LocalDate originalVedtaksDato) {
        this.originalVedtaksDato = originalVedtaksDato;
    }

    public void setBehandlingPåVent(boolean behandlingPåVent) {
        this.behandlingPåVent = behandlingPåVent;
    }

    public void setFristBehandlingPåVent(String fristBehandlingPåVent) {
        this.fristBehandlingPåVent = fristBehandlingPåVent;
    }

    public void setVenteÅrsakKode(String venteÅrsakKode) {
        this.venteÅrsakKode = venteÅrsakKode;
    }

    public void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    void setBehandlingKøet(boolean behandlingKøet) {
        this.behandlingKøet = behandlingKøet;
    }

    public void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public void setToTrinnsBehandling(boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }
}
