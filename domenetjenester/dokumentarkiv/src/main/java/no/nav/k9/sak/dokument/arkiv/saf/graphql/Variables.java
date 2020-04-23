package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Variables {

    @JsonProperty("journalpostId")
    private String journalpostId;

    @JsonProperty("saksnummer")
    private String saksnummer;

    @JsonProperty("fagsystem")
    private String fagsystem;

    public Variables(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public Variables(String saksnummer, String fagsystem) {
        this.saksnummer = saksnummer;
        this.fagsystem = fagsystem;
    }

    @Override
    public String toString() {
        return "Variables{" +
            "journalpostId='" + journalpostId + '\'' +
            ", saksnummer='" + saksnummer + '\'' +
            ", fagsystem='" + fagsystem + '\'' +
            '}';
    }
}
