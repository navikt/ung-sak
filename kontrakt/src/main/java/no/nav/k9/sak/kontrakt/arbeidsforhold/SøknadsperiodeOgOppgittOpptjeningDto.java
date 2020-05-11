package no.nav.k9.sak.kontrakt.arbeidsforhold;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SøknadsperiodeOgOppgittOpptjeningDto {


    public SøknadsperiodeOgOppgittOpptjeningDto() {
        //jackson
    }

    @JsonProperty(value = "oppgittOpptjening", required = true)
    @Valid
    @NotNull
    private OppgittOpptjeningDto oppgittOpptjening;

    @JsonProperty(value = "periodeFraSøknad", required = true)
    @Valid
    @NotNull
    private PeriodeDto periodeFraSøknad;

    public OppgittOpptjeningDto getOppgittOpptjening() {
        return oppgittOpptjening;
    }

    public void setOppgittOpptjening(OppgittOpptjeningDto oppgittOpptjening) {
        this.oppgittOpptjening = oppgittOpptjening;
    }

    public PeriodeDto getPeriodeFraSøknad() {
        return periodeFraSøknad;
    }

    public void setPeriodeFraSøknad(PeriodeDto periodeFraSøknad) {
        this.periodeFraSøknad = periodeFraSøknad;
    }
}

