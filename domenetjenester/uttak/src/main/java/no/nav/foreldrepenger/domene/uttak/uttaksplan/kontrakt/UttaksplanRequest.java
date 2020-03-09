package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttaksplanRequest {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "Saksnummer '${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String saksnummer;

    @JsonProperty(value = "behandlingId", required = true)
    @Valid
    @NotNull
    private UUID behandlingId;

    @JsonProperty(value = "andrePartersBehandlinger")
    @JsonInclude(value = Include.NON_EMPTY)
    @Valid
    private List<UUID> andrePartersBehandlinger = new ArrayList<>();

    @JsonProperty(value = "søker", required = true)
    @NotNull
    @Valid
    private Person søker;

    @JsonProperty(value = "barn", required = true)
    @NotNull
    @Valid
    private Person barn;

    @JsonProperty(value = "søknadsperioder", required = true)
    @Valid
    @NotNull
    @Size(min = 1)
    private List<Periode> søknadsperioder = new ArrayList<Periode>();

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "arbeid")
    @Valid
    private Map<UUID, UttakArbeidsforhold> arbeid = new LinkedHashMap<>();

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "tilsynsbehov")
    @Valid
    private Map<Periode, UttakTilsynsbehov> tilsynsbehov = new LinkedHashMap<>();
    
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "medlemskap")
    @Valid
    private Map<Periode, UttakMedlemskap> medlemskap = new LinkedHashMap<>();

    public Map<Periode, UttakMedlemskap> getMedlemskap() {
        return medlemskap;
    }

    public void setMedlemskap(Map<Periode, UttakMedlemskap> medlemskap) {
        this.medlemskap = medlemskap;
    }

    public List<UUID> getAndrePartersBehandlinger() {
        return andrePartersBehandlinger;
    }

    public void setAndrePartersBehandlinger(List<UUID> andrePartersBehandlinger) {
        this.andrePartersBehandlinger = andrePartersBehandlinger;
    }

    public Person getBarn() {
        return barn;
    }

    public UUID getBehandlingId() {
        return behandlingId;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public Person getSøker() {
        return søker;
    }

    public List<Periode> getSøknadsperioder() {
        return søknadsperioder;
    }

    public Map<Periode, UttakTilsynsbehov> getTilsynsbehov() {
        return tilsynsbehov;
    }

    public Map<UUID, UttakArbeidsforhold> getArbeid() {
        return arbeid;
    }

    public void setArbeid(Map<UUID, UttakArbeidsforhold> arbeid) {
        this.arbeid = arbeid;
    }

    public void setBarn(Person barn) {
        this.barn = barn;
    }

    public void setBehandlingId(UUID behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public void setSøker(Person søker) {
        this.søker = søker;
    }

    public void setSøknadsperioder(List<Periode> søknadsperioder) {
        this.søknadsperioder = søknadsperioder;
    }

    public void setTilsynsbehov(Map<Periode, UttakTilsynsbehov> tilsynsbehov) {
        this.tilsynsbehov = tilsynsbehov;
    }

    public Collection<UUID> getAlleBehandlingIder(){
        var alle = new ArrayList<>(getAndrePartersBehandlinger());
        alle.add(getBehandlingId());
        return alle;
    }
}
