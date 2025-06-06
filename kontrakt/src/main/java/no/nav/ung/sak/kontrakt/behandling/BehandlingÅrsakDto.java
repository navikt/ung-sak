package no.nav.ung.sak.kontrakt.behandling;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingÅrsakDto {

    @JsonProperty("behandlingArsakType")
    @Valid
    private BehandlingÅrsakType behandlingÅrsakType;

    @JsonProperty("manueltOpprettet")
    private boolean manueltOpprettet;

    public BehandlingÅrsakDto() {
        // trengs for deserialisering av JSON
    }

    public BehandlingÅrsakType getBehandlingArsakType() {
        return behandlingÅrsakType;
    }

    @JsonGetter
    public Boolean getErAutomatiskRevurdering() {
        return false; // Brukes kun av medlemskap og varsel revurdering i k9. Disse brukes ikke av ung
    }

    public boolean isManueltOpprettet() {
        return manueltOpprettet;
    }

    public void setBehandlingArsakType(BehandlingÅrsakType behandlingArsakType) {
        this.behandlingÅrsakType = behandlingArsakType;
    }

    public void setManueltOpprettet(boolean manueltOpprettet) {
        this.manueltOpprettet = manueltOpprettet;
    }

}
