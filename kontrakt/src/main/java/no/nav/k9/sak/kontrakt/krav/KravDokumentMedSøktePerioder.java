package no.nav.k9.sak.kontrakt.krav;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.JournalpostId;

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

    public KravDokumentMedSøktePerioder(JournalpostId journalpostId,
                                        LocalDateTime innsendingsTidspunkt,
                                        KravDokumentType type,
                                        List<SøktPeriode> søktePerioder) {
        this.journalpostId = journalpostId;
        this.innsendingsTidspunkt = innsendingsTidspunkt;
        this.type = type;
        this.søktePerioder = søktePerioder;
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
}
