package no.nav.k9.sak.kontrakt.uttak;

import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class FastsattUttakDto {

    @JsonProperty(value="behandlingUuid", required = true)
    @Valid
    @NotNull
    private UUID behandlingUuid;

    public FastsattUttakDto() {
        // for proxy
    }
    public FastsattUttakDto(UUID behandlingUuid) {
        this.behandlingUuid = Objects.requireNonNull(behandlingUuid, "behandlingUuid");
    }
    
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }
    
}
