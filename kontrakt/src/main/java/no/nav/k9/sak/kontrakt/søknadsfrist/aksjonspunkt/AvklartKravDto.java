package no.nav.k9.sak.kontrakt.søknadsfrist.aksjonspunkt;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.JournalpostId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AvklartKravDto {

    @Valid
    @NotNull
    @JsonProperty(value = "journalpostId")
    private JournalpostId journalpostId;

    @Valid
    @NotNull
    @JsonProperty(value = "godkjent")
    private Boolean godkjent;

    @Valid
    @NotNull
    @JsonProperty(value = "fraDato")
    private LocalDate fraDato;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    public AvklartKravDto() {
    }

    public AvklartKravDto(JournalpostId journalpostId, Boolean godkjent, LocalDate fraDato, String begrunnelse) {
        this.journalpostId = journalpostId;
        this.godkjent = godkjent;
        this.fraDato = fraDato;
        this.begrunnelse = begrunnelse;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Boolean getGodkjent() {
        return godkjent;
    }

    public LocalDate getFraDato() {
        return fraDato;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvklartKravDto that = (AvklartKravDto) o;
        return Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() {
        return "AvklartKravDto{" +
            "journalpostId=" + journalpostId +
            '}';
    }
}
