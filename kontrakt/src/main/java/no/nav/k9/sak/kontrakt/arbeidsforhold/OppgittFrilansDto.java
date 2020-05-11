package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OppgittFrilansDto {


    public OppgittFrilansDto() {
        //jackson
    }

    @Valid
    @Size(max = 100)
    @JsonProperty("oppgittFrilansoppdrag")
    private List<OppgittFrilansoppdragDto> oppgittFrilansoppdrag = Collections.emptyList();


    public List<OppgittFrilansoppdragDto> getOppgittFrilansoppdrag() {
        return oppgittFrilansoppdrag;
    }

    public void setOppgittFrilansoppdrag(List<OppgittFrilansoppdragDto> oppgittFrilansoppdrag) {
        this.oppgittFrilansoppdrag = oppgittFrilansoppdrag;
    }
}
