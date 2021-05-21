package no.nav.k9.sak.kontrakt.søknadsfrist.aksjonspunkt;

import java.time.LocalDate;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.JournalpostId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AvklartDokument {

    @Valid
    @NotNull
    @JsonProperty("jorunalpostId")
    private JournalpostId journalpostId;

    @NotNull
    @JsonProperty("godkjent")
    private Boolean godkjent;

    @NotNull
    @JsonProperty("godkjentFraDato")
    private LocalDate godkjentFraDato;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonCreator
    public AvklartDokument(@Valid
                           @NotNull
                           @JsonProperty(value = "jorunalpostId", required = true) JournalpostId journalpostId,
                           @NotNull
                           @JsonProperty(value = "godkjent", required = true) Boolean godkjent,
                           @NotNull
                           @JsonProperty(value = "godkjentFraDato", required = true) LocalDate godkjentFraDato,
                           @Size(max = 4000)
                           @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
                           @JsonProperty(value = "begrunnelse", required = true) String begrunnelse) {
        this.journalpostId = journalpostId;
        this.godkjent = godkjent;
        this.godkjentFraDato = godkjentFraDato;
        this.begrunnelse = begrunnelse;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Boolean getGodkjent() {
        return godkjent;
    }

    public LocalDate getGodkjentFraDato() {
        return godkjentFraDato;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvklartDokument that = (AvklartDokument) o;
        return Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }
}
