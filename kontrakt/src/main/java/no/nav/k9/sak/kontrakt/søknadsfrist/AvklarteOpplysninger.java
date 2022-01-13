package no.nav.k9.sak.kontrakt.søknadsfrist;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AvklarteOpplysninger {

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

    public AvklarteOpplysninger() {
    }

    public AvklarteOpplysninger(Boolean godkjent, LocalDate fraDato, String begrunnelse) {
        this.godkjent = godkjent;
        this.fraDato = fraDato;
        this.begrunnelse = begrunnelse;
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
}
