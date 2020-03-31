package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ÅrskvantumRequest {
    @JsonProperty(value = "behandlingId", required = true)
    @Valid
    @NotNull
    private String behandlingId;

    @JsonProperty(value = "aktørid", required = true)
    @Valid
    @NotNull
    private String aktørid;

    @JsonProperty(value = "uttakperioder")
    @Valid
    @Size(max = 1000)
    private List<UttaksperiodeOmsorgspenger> uttaksperioder = new ArrayList<>();


    public String getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(String behandlingId) {
        this.behandlingId = behandlingId;
    }

    public List<UttaksperiodeOmsorgspenger> getUttaksperioder() {
        return uttaksperioder;
    }

    public void setUttaksperioder(List<UttaksperiodeOmsorgspenger> uttaksperioder) {
        this.uttaksperioder = uttaksperioder;
    }
}
