package no.nav.ung.sak.kontrakt.behandling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.PUBLIC_ONLY, fieldVisibility = Visibility.ANY)
@JsonInclude(Include.NON_NULL)
public class BehandlingIdListe {

    @JsonProperty(value = "behandlinger", required = true)
    @NotEmpty
    @Valid
    @Size(min = 1, max = 1000)
    private List<BehandlingIdDto> behandlinger = new ArrayList<>();

    public BehandlingIdListe(@JsonProperty(value = "behandlinger", required = true) @NotEmpty @Valid List<BehandlingIdDto> behandlinger) {
        this.behandlinger = Objects.requireNonNull(behandlinger, "behandlinger");
    }

    public List<BehandlingIdDto> getBehandlinger() {
        return Collections.unmodifiableList(behandlinger);
    }

    @AbacAttributt(value = "behandlingUuid")
    public List<UUID> getBehandlingUuid() {
        return behandlinger.stream().map(BehandlingIdDto::getBehandlingUuid).collect(Collectors.toList());
    }
}
