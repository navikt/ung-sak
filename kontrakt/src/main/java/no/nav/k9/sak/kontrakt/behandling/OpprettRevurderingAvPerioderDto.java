package no.nav.k9.sak.kontrakt.behandling;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OpprettRevurderingAvPerioderDto {

    @JsonProperty(value = "perioder", required = true)
    @NotNull
    @Valid
    @Size(min = 1)
    private Map<@NotNull @Valid Periode, @NotNull BehandlingÅrsakType> behandlingArsakType;

    @JsonProperty(value = "behandlingType", required = true)
    @NotNull
    private BehandlingType behandlingType;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    public OpprettRevurderingAvPerioderDto() {
        //
    }

    public OpprettRevurderingAvPerioderDto(Map<@NotNull @Valid Periode, @NotNull BehandlingÅrsakType> behandlingArsakType,
                                           BehandlingType behandlingType,
                                           Saksnummer saksnummer) {
        this.behandlingArsakType = behandlingArsakType;
        this.behandlingType = behandlingType;
        this.saksnummer = saksnummer;
    }

    public Map<Periode, BehandlingÅrsakType> getBehandlingArsakType() {
        return behandlingArsakType;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

}
