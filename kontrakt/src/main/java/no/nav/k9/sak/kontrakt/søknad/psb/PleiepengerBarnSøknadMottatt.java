package no.nav.k9.sak.kontrakt.søknad.psb;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class PleiepengerBarnSøknadMottatt {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private final Saksnummer saksnummer;

    @JsonCreator
    public PleiepengerBarnSøknadMottatt(@JsonProperty(value = "saksnummer", required = true)
                                        @NotNull
                                        @Valid Saksnummer saksnummer) {
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }
}
