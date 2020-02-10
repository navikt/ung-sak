package no.nav.k9.sak.kontrakt.mottak;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForsendelseStatusData {

    @JsonProperty(value = "forsendelseStatus", required = true)
    @NotNull
    @Valid
    private ForsendelseStatus forsendelseStatus;

    /** Joark journalpostid. */
    @JsonProperty(value = "journalpostId")
    @Valid
    private JournalpostId journalpostId;

    /** GSAK Saksnummer. (samme som Fagsak#saksnummer). */
    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    public ForsendelseStatusData(ForsendelseStatus forsendelseStatus) {
        this.forsendelseStatus = forsendelseStatus;
    }

    public ForsendelseStatus getForsendelseStatus() {
        return forsendelseStatus;
    }

    public void setForsendelseStatus(ForsendelseStatus forsendelseStatus) {
        this.forsendelseStatus = forsendelseStatus;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(JournalpostId journalpostId) {
        this.journalpostId = journalpostId;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }
}
