package no.nav.k9.sak.kontrakt.produksjonstyring;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;
import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class SøkeSakEllerBrukerDto {

    @JsonProperty(value = "searchString", required = true)
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String searchString;

    SøkeSakEllerBrukerDto() {
    }

    public SøkeSakEllerBrukerDto(String searchString) {
        this.searchString = searchString;
    }

    public SøkeSakEllerBrukerDto(Saksnummer saksnummer) {
        this.searchString = saksnummer.getVerdi();
    }

    public String getSearchString() {
        return searchString;
    }

    @AbacAttributt("saksnummer")
    public String getSaksnummer() {
        return !antattFnr() ? searchString : null;
    }

    @AbacAttributt("fnr")
    public String getFnr() {
        return antattFnr() ? searchString : null;
    }

    @AbacAttributt("fnrSok")
    public String getFnrSok() {
        return antattFnr() ? searchString : null;
    }

    private boolean antattFnr() {
        return searchString.length() == 11 /* guess - fødselsnummer */;
    }

}
