package no.nav.k9.sak.kontrakt.behandling;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

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
        if (behandlingÅrsakType == null) {
            return false;
        }
        return BehandlingÅrsakType.årsakerForAutomatiskRevurdering().stream().anyMatch(årsak -> årsak.equals(this.behandlingÅrsakType));
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
