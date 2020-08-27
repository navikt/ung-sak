package no.nav.k9.sak.kontrakt.behandling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.PUBLIC_ONLY, fieldVisibility = Visibility.ANY)
@JsonInclude(Include.NON_NULL)
public class BehandlingIdListe {

    @JsonProperty(value = "behandlinger", required = true)
    @NotEmpty
    @Valid
    private List<BehandlingIdDto> behandlinger = new ArrayList<>();

    public BehandlingIdListe(@JsonProperty(value = "behandlinger", required = true) @NotEmpty @Valid List<BehandlingIdDto> behandlinger) {
        this.behandlinger = Objects.requireNonNull(behandlinger, "behandlinger");
    }

    public List<BehandlingIdDto> getBehandlinger() {
        return Collections.unmodifiableList(behandlinger);
    }
}
