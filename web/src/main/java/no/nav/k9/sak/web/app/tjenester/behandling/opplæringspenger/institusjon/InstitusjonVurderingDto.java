package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.institusjon;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InstitusjonVurderingDto {

    @JsonProperty(value = "journalpostId", required = true)
    @Valid
    @NotNull
    private JournalpostIdDto journalpostId;

    @JsonProperty(value = "resultat", required = true)
    @Valid
    @NotNull
    private Resultat resultat;

    @JsonProperty(value = "begrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    @NotNull
    private String begrunnelse;

    public InstitusjonVurderingDto(JournalpostIdDto journalpostId, Resultat resultat, String begrunnelse) {
        this.journalpostId = journalpostId;
        this.resultat = resultat;
        this.begrunnelse = begrunnelse;
    }

    public JournalpostIdDto getJournalpostId() {
        return journalpostId;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
