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
public class OppgittOpptjeningDto {


    public OppgittOpptjeningDto() {
        //Jackson
    }

    @Valid
    @Size(max = 100)
    @JsonProperty(value = "oppgittEgenNæring")
    private List<OppgittEgenNæringDto> oppgittEgenNæring = Collections.emptyList();

    @Valid
    @JsonProperty(value = "oppgittFrilans")
    private OppgittFrilansDto oppgittFrilans;


    public void setOppgittEgenNæring(List<OppgittEgenNæringDto> oppgittEgenNæring) {
        this.oppgittEgenNæring = oppgittEgenNæring;
    }

    public void setOppgittFrilans(OppgittFrilansDto oppgittFrilans) {
        this.oppgittFrilans = oppgittFrilans;
    }

    public List<OppgittEgenNæringDto> getOppgittEgenNæring() {
        return oppgittEgenNæring;
    }

    public OppgittFrilansDto getOppgittFrilans() {
        return oppgittFrilans;
    }
}
