package no.nav.k9.sak.kontrakt.fagsak;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.PersonIdent;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class RelaterteSakerDTO {

    @JsonProperty(value = "ytelseType", required = true)
    @Valid
    @NotNull
    private FagsakYtelseType ytelseType;

    @JsonProperty(value = "bruker", required = false)
    @Valid
    private PersonIdent pleietrengendeIdent;

    @JsonInclude(value = Include.NON_ABSENT)
    @JsonProperty(value = "pleietrengendeIdenter", required = false)
    @Size(max = 20)
    @Valid
    private List<RelatertSøkerDTO> relaterteSøkere;


    protected RelaterteSakerDTO() {
        //
    }

    public RelaterteSakerDTO(
            @Valid @NotNull FagsakYtelseType ytelseType,
            @Valid @NotNull PersonIdent pleietrengendeIdent,
            @Valid @NotNull List<RelatertSøkerDTO> relaterteSøkere) {
        this.ytelseType = ytelseType;
        this.pleietrengendeIdent = pleietrengendeIdent;
        this.relaterteSøkere = relaterteSøkere;
    }
}
