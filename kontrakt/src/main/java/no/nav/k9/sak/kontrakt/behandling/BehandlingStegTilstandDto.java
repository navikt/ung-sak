package no.nav.k9.sak.kontrakt.behandling;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingStegTilstandDto {

    @JsonProperty(value = "stegStatus", required = true)
    @Valid
    private BehandlingStegStatus stegStatus;

    @JsonProperty(value = "stegType", required = true)
    @Valid
    private BehandlingStegType stegType;

    @JsonProperty(value = "tidsstempel")
    @Valid
    private ZonedDateTime tidsstempel;

    @JsonCreator
    public BehandlingStegTilstandDto(@JsonProperty(value = "stegType", required = true) @Valid BehandlingStegType stegType,
                                     @JsonProperty(value = "stegStatus", required = true) @Valid BehandlingStegStatus stegStatus,
                                     @JsonProperty(value = "tidsstempel") @Valid ZonedDateTime tidsstempel) {
        this.stegType = stegType;
        this.stegStatus = stegStatus;
        this.tidsstempel = tidsstempel;
    }

    public BehandlingStegTilstandDto(BehandlingStegType stegType, BehandlingStegStatus stegStatus, LocalDateTime tidsstempel) {
        this(stegType, stegStatus, tidsstempel.atZone(ZoneId.systemDefault()));
    }

    public BehandlingStegStatus getStegStatus() {
        return stegStatus;
    }

    public BehandlingStegType getStegType() {
        return stegType;
    }

    public ZonedDateTime getTidsstempel() {
        return tidsstempel;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<stegType=" + stegType
            + ", stegStatus=" + stegStatus
            + ">";
    }
}
