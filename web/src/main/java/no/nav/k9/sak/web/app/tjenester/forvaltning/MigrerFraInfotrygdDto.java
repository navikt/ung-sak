package no.nav.k9.sak.web.app.tjenester.forvaltning;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class MigrerFraInfotrygdDto {


    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @NotNull
    @Valid
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Size(max = 19)
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String saksnummer;

    public MigrerFraInfotrygdDto() {
    }

    public MigrerFraInfotrygdDto(LocalDate skjæringstidspunkt, String saksnummer) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.saksnummer = saksnummer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return new Saksnummer(saksnummer);
    }


}
