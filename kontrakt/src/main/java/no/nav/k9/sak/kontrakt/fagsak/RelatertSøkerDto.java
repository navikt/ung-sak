package no.nav.k9.sak.kontrakt.fagsak;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class RelatertSøkerDto {

    @JsonProperty(value = "søkerIdent", required = true)
    @NotNull
    @Valid
    private PersonIdent søkerIdent;

    @JsonProperty(value = "søkerNavn", required = true)
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{P}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String søkerNavn;

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "åpenBehandling")
    @NotNull
    private Boolean åpenBehandling;

    protected RelatertSøkerDto() {
        //
    }

    public RelatertSøkerDto(
        @Valid @NotNull PersonIdent søkerIdent,
        @Valid @NotNull String søkerNavn,
        @Valid @NotNull Saksnummer saksnummer,
        @NotNull Boolean åpenBehandling) {
        this.søkerIdent = søkerIdent;
        this.søkerNavn = søkerNavn;
        this.saksnummer = saksnummer;
        this.åpenBehandling = åpenBehandling;
    }

    public PersonIdent getSøkerIdent() {
        return søkerIdent;
    }

    public String getSøkerNavn() {
        return søkerNavn;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Boolean getHarÅpenBehandling() {
        return åpenBehandling;
    }
}
