package no.nav.k9.sak.kontrakt.uttak;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UttaksPlanOmsorgspengerAktivitet {

    @JsonProperty(value = "arbeidsforhold", required = true)
    @Valid
    @NotNull
    private UttakArbeidsforhold arbeidsforhold;

    @JsonProperty(value = "uttaksperioder", required = true)
    @Valid
    @NotNull
    private List<UttaksperiodeOmsorgspenger> uttaksperioder;


    public UttakArbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public void setArbeidsforhold(UttakArbeidsforhold arbeidsforhold) {
        this.arbeidsforhold = arbeidsforhold;
    }

    public List<UttaksperiodeOmsorgspenger> getUttaksperioder() {
        return uttaksperioder;
    }

    public void setUttaksperioder(List<UttaksperiodeOmsorgspenger> uttaksperioder) {
        this.uttaksperioder = uttaksperioder;
    }
}
