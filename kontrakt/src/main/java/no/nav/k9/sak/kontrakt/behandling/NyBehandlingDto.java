package no.nav.k9.sak.kontrakt.behandling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class NyBehandlingDto {

    @JsonProperty(value = "behandlingArsakType", required = true)
    @NotNull
    @Valid
    private BehandlingÅrsakType behandlingArsakType;

    @JsonProperty(value = "behandlingType", required = true)
    @NotNull
    private BehandlingType behandlingType;

    @JsonProperty(value = "nyBehandlingEtterKlage")
    private boolean nyBehandlingEtterKlage;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    public NyBehandlingDto() {
        //
    }

    public BehandlingÅrsakType getBehandlingArsakType() {
        return behandlingArsakType;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public boolean getNyBehandlingEtterKlage() {
        return nyBehandlingEtterKlage;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public void setBehandlingArsakType(BehandlingÅrsakType behandlingArsakType) {
        this.behandlingArsakType = behandlingArsakType;
    }

    public void setBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
    }

    public void setNyBehandlingEtterKlage(boolean nyBehandlingEtterKlage) {
        this.nyBehandlingEtterKlage = nyBehandlingEtterKlage;
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

}
