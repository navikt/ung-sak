package no.nav.k9.sak.kontrakt.produksjonsstyring.los;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BehandlingMedFagsakDto {

    @JsonProperty(value = "sakstype", required = true)
    @Valid
    @NotNull
    private FagsakYtelseType sakstype;

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "behandlingResultatType")
    @Valid
    private BehandlingResultatType behandlingResultatType;

    public FagsakYtelseType getSakstype() {
        return sakstype;
    }

    public void setSakstype(FagsakYtelseType sakstype) {
        this.sakstype = sakstype;
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public void setBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
        this.behandlingResultatType = behandlingResultatType;
    }
}
