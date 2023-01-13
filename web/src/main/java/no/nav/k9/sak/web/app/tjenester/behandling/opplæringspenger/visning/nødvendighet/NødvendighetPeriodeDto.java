package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.nødvendighet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class NødvendighetPeriodeDto {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "journalpostId", required = true)
    @Valid
    @NotNull
    private JournalpostIdDto journalpostId;

    public NødvendighetPeriodeDto(Periode periode, JournalpostIdDto journalpostId) {
        this.periode = periode;
        this.journalpostId = journalpostId;
    }

    public Periode getPeriode() {
        return periode;
    }

    public JournalpostIdDto getJournalpostId() {
        return journalpostId;
    }
}
