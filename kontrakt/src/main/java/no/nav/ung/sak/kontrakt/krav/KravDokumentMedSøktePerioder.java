package no.nav.ung.sak.kontrakt.krav;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

import no.nav.ung.sak.typer.JournalpostId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KravDokumentMedSøktePerioder {

    @Valid
    @JsonProperty("journalpostId")
    private JournalpostId journalpostId;

    @Valid
    @NotNull
    @JsonProperty("innsendingsTidspunkt")
    private LocalDateTime innsendingsTidspunkt;

    @Valid
    @NotNull
    @JsonProperty("type")
    private KravDokumentType type;

    @Valid
    @Size
    @JsonProperty("søktePerioder")
    private List<SøktPeriode> søktePerioder;

    @Valid
    @Size(max = 100)
    @Pattern(regexp = "^\\p{L}+$")
    @JsonProperty("kildesystem")
    private String kildesystem;

    @JsonCreator
    public KravDokumentMedSøktePerioder(@JsonProperty(value = "journalpostId", required = true) JournalpostId journalpostId,
                                        @JsonProperty("innsendingsTidspunkt") LocalDateTime innsendingsTidspunkt,
                                        @JsonProperty("type") KravDokumentType type,
                                        @JsonProperty("søktePerioder") List<SøktPeriode> søktePerioder,
                                        @JsonProperty("kildesystem") String kildesystem) {
        this.journalpostId = journalpostId;
        this.innsendingsTidspunkt = innsendingsTidspunkt;
        this.type = type;
        this.søktePerioder = søktePerioder;
        this.kildesystem = kildesystem;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public LocalDateTime getInnsendingsTidspunkt() {
        return innsendingsTidspunkt;
    }

    public KravDokumentType getType() {
        return type;
    }

    public List<SøktPeriode> getSøktePerioder() {
        return søktePerioder;
    }

    public String getKildesystem() {
        return kildesystem;
    }
}
