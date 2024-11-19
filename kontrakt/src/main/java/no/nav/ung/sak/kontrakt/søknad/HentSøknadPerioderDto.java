package no.nav.ung.sak.kontrakt.søknad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.PersonIdent;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class HentSøknadPerioderDto {

    @JsonProperty(value = "ytelseType", required = true)
    @Valid
    @NotNull
    private FagsakYtelseType ytelseType;

    @JsonProperty(value = "bruker", required = true)
    @Valid
    private PersonIdent brukerIdent;

    @JsonProperty(value = "pleietrengende")
    @Valid
    private PersonIdent pleietrengendeIdent;


    protected HentSøknadPerioderDto() {
        //
    }

    public HentSøknadPerioderDto(@Valid @NotNull FagsakYtelseType ytelseType,
                                 @Valid @NotNull PersonIdent brukerIdent,
                                 @Valid PersonIdent pleietrengendeIdent) {
        this.ytelseType = ytelseType;
        this.brukerIdent = brukerIdent;
        this.pleietrengendeIdent = pleietrengendeIdent;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public PersonIdent getBruker() {
        return brukerIdent;
    }

    public PersonIdent getPleietrengende() {
        return pleietrengendeIdent;
    }
}
