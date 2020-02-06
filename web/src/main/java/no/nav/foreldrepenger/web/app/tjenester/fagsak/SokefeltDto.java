package no.nav.foreldrepenger.web.app.tjenester.fagsak;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class SokefeltDto implements AbacDto {

    @JsonProperty(value="searchString", required = true)
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String searchString;

    @SuppressWarnings("unused")
    private SokefeltDto() { // NOSONAR
    }

    public SokefeltDto(String searchString) {
        this.searchString = searchString;
    }

    public SokefeltDto(Saksnummer saksnummer) {
        this.searchString = saksnummer.getVerdi();
    }

    public String getSearchString() {
        return searchString;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        AbacDataAttributter attributter = AbacDataAttributter.opprett();
        if (searchString.length() == 11 /* guess - fødselsnummer */) {
            attributter
                    .leggTil(AppAbacAttributtType.FNR, searchString)
                    .leggTil(AppAbacAttributtType.SAKER_MED_FNR, searchString);
        } else {
            attributter.leggTil(AppAbacAttributtType.SAKSNUMMER, searchString);
        }
        return attributter;
    }

}
