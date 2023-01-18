package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.institusjon;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InstitusjonPeriodeDto {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "institusjon", required = true)
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    @NotNull
    private String institusjon;

    @JsonProperty(value = "journalpostId", required = true)
    @Valid
    @NotNull
    private JournalpostIdDto journalpostId;

    public InstitusjonPeriodeDto(Periode periode, String institusjon, JournalpostIdDto journalpostId) {
        this.periode = periode;
        this.institusjon = institusjon;
        this.journalpostId = journalpostId;
    }

    public Periode getPeriode() {
        return periode;
    }

    public String getInstitusjon() {
        return institusjon;
    }

    public JournalpostIdDto getJournalpostId() {
        return journalpostId;
    }
}
