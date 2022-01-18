package no.nav.k9.sak.kontrakt.uttak;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class FastsattUttakDto {

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    @NotNull
    private UUID behandlingUuid;

    @JsonInclude(value = Include.ALWAYS)
    @JsonProperty(value = "aktiviteter")
    @Valid
    @Size(max = 200)
    private List<@NotNull UttakAktivitetPeriodeDto> aktiviteter;

    @JsonCreator
    public FastsattUttakDto(@JsonProperty(value = "behandlingUuid", required = true) UUID behandlingUuid,
                            @JsonProperty(value = "aktiviteter") List<@NotNull UttakAktivitetPeriodeDto> aktiviteter) {
        this.aktiviteter = aktiviteter == null ? Collections.emptyList() : List.copyOf(aktiviteter);
        this.behandlingUuid = Objects.requireNonNull(behandlingUuid, "behandlingUuid");
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public List<UttakAktivitetPeriodeDto> getAktiviteter() {
        return aktiviteter;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (FastsattUttakDto) obj;
        return Objects.equals(behandlingUuid, other.behandlingUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingUuid);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<behandlingUuid=" + behandlingUuid + ", aktiviteter=" + aktiviteter + ">";
    }
}
