package no.nav.k9.sak.kontrakt.uttak.overstyring;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverstyrtUttakDto {

    @JsonProperty(value = "overstyringer")
    @Valid
    @NotNull
    @Size(max = 100)
    private List<OverstyrUttakPeriodeDto> overstyringer;

    @JsonProperty(value = "arbeidsgiverOversikt")
    @Valid
    @NotNull
    private ArbeidsgiverOversiktDto arbeidsgiverOversikt;

    public OverstyrtUttakDto() {
    }

    public OverstyrtUttakDto(List<OverstyrUttakPeriodeDto> overstyringer, ArbeidsgiverOversiktDto arbeidsgiverOversikt) {
        this.overstyringer = overstyringer;
        this.arbeidsgiverOversikt = arbeidsgiverOversikt;
    }

    public List<OverstyrUttakPeriodeDto> getOverstyringer() {
        return overstyringer;
    }

    public ArbeidsgiverOversiktDto getArbeidsgiverOversikt() {
        return arbeidsgiverOversikt;
    }
}
