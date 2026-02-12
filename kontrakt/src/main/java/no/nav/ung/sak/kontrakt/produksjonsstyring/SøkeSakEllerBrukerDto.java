package no.nav.ung.sak.kontrakt.produksjonsstyring;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.abac.AppAbacAttributt;
import no.nav.ung.sak.abac.AppAbacAttributtType;
import no.nav.ung.sak.abac.StandardAbacAttributt;
import no.nav.ung.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class SøkeSakEllerBrukerDto {

    @JsonProperty(value = "ytelseType")
    @Valid
    private FagsakYtelseType ytelseType = FagsakYtelseType.UNGDOMSYTELSE; //TODO fjern default verdi og sett @NotNull når frontend sender inn verdien

    @JsonProperty(value = "searchString", required = true)
    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9]*$")
    private String searchString;

    public SøkeSakEllerBrukerDto() {
    }

    @Deprecated(forRemoval = true)
    public SøkeSakEllerBrukerDto(Saksnummer saksnummer) {
        this.searchString = saksnummer.getVerdi();
    }

    @Deprecated(forRemoval = true)
    public SøkeSakEllerBrukerDto(String searchString) {
        this.searchString = searchString;
    }

    public SøkeSakEllerBrukerDto(Saksnummer saksnummer, FagsakYtelseType ytelseType) {
        this.ytelseType = ytelseType;
        this.searchString = saksnummer.getVerdi();
    }

    public SøkeSakEllerBrukerDto(String searchString, FagsakYtelseType ytelseType) {
        this.ytelseType = ytelseType;
        this.searchString = searchString;
    }


    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    public String getFnr() {
        return antattFnr() ? searchString : null;
    }

    @AppAbacAttributt(AppAbacAttributtType.SAKER_MED_FNR)
    public String getFnrSok() {
        return antattFnr() ? searchString : null;
    }

    @StandardAbacAttributt(StandardAbacAttributtType.SAKSNUMMER)
    public Saksnummer getSaksnummer() {
        return !antattFnr() ? new Saksnummer(searchString) : null;
    }

    @AppAbacAttributt(AppAbacAttributtType.YTELSETYPE)
    public String getYtelseTypeKode() {
        return ytelseType.getKode();
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    @AssertTrue
    public boolean erStøttetYtelseType() {
        return ytelseType == null
            || ytelseType == FagsakYtelseType.UNGDOMSYTELSE
            || ytelseType == FagsakYtelseType.AKTIVITETSPENGER;
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

