package no.nav.k9.sak.kontrakt.behandling;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingIdVersjonDto extends BehandlingIdDto {

    @JsonAlias("versjon")
    @JsonProperty(value="behandlingVersjon")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' +
            (getSaksnummer() == null ? "" : "saksnummer=" + getSaksnummer() + ", ") +
            (getBehandlingVersjon() != null ? "behandlingVersjon=" + getBehandlingVersjon() + ", " : "") +
            (getBehandlingId() != null ? "behandlingId=" + getBehandlingId() : "") +
            (getBehandlingId() != null && getBehandlingUuid() != null ? ", " : "") +
            (getBehandlingUuid() != null ? "behandlingUuid=" + getBehandlingUuid() : "") +
            '>';
    }
}
