package no.nav.k9.sak.kontrakt.person;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.kodeverk.person.PersonstatusType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AvklartPersonstatus {

    private PersonstatusType orginalPersonstatus;
    private PersonstatusType overstyrtPersonstatus;

    @JsonCreator
    public AvklartPersonstatus(@JsonProperty("orginalPersonstatus") PersonstatusType orginalPersonstatus,
                               @JsonProperty("overstyrtPersonstatus") PersonstatusType overstyrtPersonstatus) {
        this.orginalPersonstatus = orginalPersonstatus;
        this.overstyrtPersonstatus = overstyrtPersonstatus;
    }

    public PersonstatusType getOrginalPersonstatus() {
        return orginalPersonstatus;
    }

    public PersonstatusType getOverstyrtPersonstatus() {
        return overstyrtPersonstatus;
    }
}
