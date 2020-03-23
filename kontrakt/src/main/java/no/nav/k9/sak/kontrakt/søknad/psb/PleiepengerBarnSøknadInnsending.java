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
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class PleiepengerBarnSøknadInnsending {

    @JsonProperty(value = "saksnummer", required = true)
    @NotNull
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "søknad", required = true)
    @NotNull
    @Valid
    private PleiepengerBarnSøknad søknad;

    public PleiepengerBarnSøknadInnsending() {
        //
    }

    @JsonCreator
    public PleiepengerBarnSøknadInnsending(@JsonProperty(value = "saksnummer", required = true) @NotNull @Valid Saksnummer saksnummer,
                                           @JsonProperty(value = "søknad", required = true) @NotNull @Valid PleiepengerBarnSøknad søknad) {
        this.saksnummer = Objects.requireNonNull(saksnummer, "saksnummer");
        this.søknad = søknad;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public PleiepengerBarnSøknad getSøknad() {
        return søknad;
    }

    public void setSøknad(PleiepengerBarnSøknad søknad) {
        this.søknad = søknad;
    }
}
