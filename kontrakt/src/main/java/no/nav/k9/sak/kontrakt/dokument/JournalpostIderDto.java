package no.nav.k9.sak.kontrakt.dokument;

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
public class JournalpostIderDto {


    @JsonProperty(value = "journalpostIder")
    @Size(max = 100)
    @Valid
    private List<JournalpostIdDto> journalpostIder;


    public JournalpostIderDto() {
       // cdi
    }

    public List<JournalpostIdDto> getJournalpostIder() {
        return journalpostIder;
    }

    public void setJournalpostIder(List<JournalpostIdDto> journalpostIder) {
        this.journalpostIder = journalpostIder;
    }
}
