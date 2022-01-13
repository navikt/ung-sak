package no.nav.k9.sak.kontrakt.dokument;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

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

    @JsonProperty(value = "journalpostIderBarn")
    @Size(max = 100)
    @Valid
    private List<JournalpostIdDto> journalpostIderBarn;

    public JournalpostIderDto() {
       // cdi
    }

    public List<JournalpostIdDto> getJournalpostIder() {
        return journalpostIder;
    }

    public void setJournalpostIder(List<JournalpostIdDto> journalpostIder) {
        this.journalpostIder = journalpostIder;
    }

    public List<JournalpostIdDto> getJournalpostIderBarn() {
        return journalpostIderBarn;
    }

    public void setJournalpostIderBarn(List<JournalpostIdDto> journalpostIderBarn) {
        this.journalpostIderBarn = journalpostIderBarn;
    }
}
