package no.nav.k9.sak.kontrakt.frisinn;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SøknadsperiodeOgOppgittOpptjeningV2Dto {


    @JsonProperty(value = "førSøkerPerioden", required = true)
    @Valid
    @NotNull
    private OppgittOpptjeningDto førSøkerPerioden;

    @JsonProperty(value = "måneder", required = true)
    @Valid
    @Size(min = 1)
    private List<PeriodeMedSNOgFLDto> måneder;

    public SøknadsperiodeOgOppgittOpptjeningV2Dto() {
        //jackson
    }

    public OppgittOpptjeningDto getFørSøkerPerioden() {
        return førSøkerPerioden;
    }

    public void setFørSøkerPerioden(OppgittOpptjeningDto førSøkerPerioden) {
        this.førSøkerPerioden = førSøkerPerioden;
    }

    public List<PeriodeMedSNOgFLDto> getMåneder() {
        return måneder;
    }

    public void setMåneder(List<PeriodeMedSNOgFLDto> måneder) {
        this.måneder = måneder;
    }
}

