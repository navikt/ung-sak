package no.nav.k9.sak.kontrakt.søknad.psb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape=Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class PleiepengerBarnSoknadMottatt {
    private final Saksnummer saksnummer;

    public PleiepengerBarnSoknadMottatt(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }
}