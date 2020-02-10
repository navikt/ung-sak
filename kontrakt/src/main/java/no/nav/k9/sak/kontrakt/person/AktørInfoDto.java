package no.nav.k9.sak.kontrakt.person;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.behandling.FagsakDto;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AktørInfoDto {

    @JsonAlias("aktørId")
    @JsonProperty(value = "aktoerId")
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "person")
    @Valid
    private PersonDto person;

    @JsonProperty(value = "fagsaker")
    @Size(max = 100)
    @Valid
    private List<FagsakDto> fagsaker;

    public AktørId getAktørId() {
        return aktørId;
    }

    public void setAktørId(AktørId aktoerId) {
        this.aktørId = aktoerId;
    }

    public void setFagsaker(List<FagsakDto> fagsaker) {
        this.fagsaker = fagsaker;
    }

    public PersonDto getPerson() {
        return person;
    }

    public List<FagsakDto> getFagsaker() {
        return fagsaker;
    }

    public void setPerson(PersonDto person) {
        this.person = person;
    }

}
