package no.nav.ung.domenetjenester.arkiv.dok.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FerdigstillJournalpostRequest {

    @JsonProperty("journalfoerendeEnhet")
    @NotNull
    private String journalfoerendeEnhet;

    @JsonCreator
    public FerdigstillJournalpostRequest(@JsonProperty("journalfoerendeEnhet") @NotNull String journalfoerendeEnhet) {
        this.journalfoerendeEnhet = journalfoerendeEnhet;
    }

    public String getJournalfoerendeEnhet() {
        return journalfoerendeEnhet;
    }
}
