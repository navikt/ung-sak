package no.nav.ung.sak.kontrakt.produksjonsstyring;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class SøkeSakEllerBrukerDto {

    @JsonProperty(value = "searchString", required = true)
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String searchString;

    public SøkeSakEllerBrukerDto() {
    }

    public SøkeSakEllerBrukerDto(Saksnummer saksnummer) {
        this.searchString = saksnummer.getVerdi();
    }

    public SøkeSakEllerBrukerDto(String searchString) {
        this.searchString = searchString;
    }

    @AbacAttributt("fnr")
    public String getFnr() {
        return antattFnr() ? searchString : null;
    }

    @AbacAttributt("fnrSok")
    public String getFnrSok() {
        return antattFnr() ? searchString : null;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return !antattFnr() ? new Saksnummer(searchString) : null;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    private boolean antattFnr() {
        return searchString == null || searchString.length() == 11 /* guess - fødselsnummer */;
    }

}
