package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
public class SaksnummerDto implements AbacDto {

    @JsonProperty("saksnummer")
    @NotNull
    @Size(
        max = 19
    )
    @Pattern(
        regexp = "^[a-zA-Z0-9]*$"
    )
    private final String saksnummer;


    public SaksnummerDto(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public SaksnummerDto(Saksnummer saksnummer) {
        this.saksnummer = saksnummer.getVerdi();
    }


    public String getVerdi() {
        return saksnummer;
    }

    @Override
    public String toString() {
        return "SaksnummerDto{" +
            "saksnummer='" + saksnummer + '\'' +
            '}';
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, getVerdi());
    }
}
