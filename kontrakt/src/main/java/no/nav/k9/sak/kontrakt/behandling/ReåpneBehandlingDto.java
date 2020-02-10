package no.nav.k9.sak.kontrakt.behandling;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ReåpneBehandlingDto {

    @JsonProperty(value = "behandlingId")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    private Long behandlingId;

    @JsonAlias("versjon")
    @JsonProperty(value = "behandlingVersjon")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    @Override
    public String toString() {
        return "BehandlingIdDto{" + "behandlingId=" + behandlingId + '}';
    }

}
