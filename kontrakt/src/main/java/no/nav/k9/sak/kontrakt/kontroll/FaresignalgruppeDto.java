package no.nav.k9.sak.kontrakt.kontroll;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.risikoklassifisering.Kontrollresultat;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FaresignalgruppeDto {

    @JsonProperty(value = "kontrollresultat", required = true)
    @NotNull
    @Valid
    private Kontrollresultat kontrollresultat;

    @JsonProperty(value = "faresignaler")
    @Valid
    @Size(max = 50)
    private List<@NotNull @Size(max = 5000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String> faresignaler;

    public FaresignalgruppeDto() {
        //
    }

    public Kontrollresultat getKontrollresultat() {
        return kontrollresultat;
    }

    public void setKontrollresultat(Kontrollresultat kontrollresultat) {
        this.kontrollresultat = kontrollresultat;
    }

    public List<String> getFaresignaler() {
        return faresignaler;
    }

    public void setFaresignaler(List<String> faresignaler) {
        this.faresignaler = faresignaler;
    }
}
