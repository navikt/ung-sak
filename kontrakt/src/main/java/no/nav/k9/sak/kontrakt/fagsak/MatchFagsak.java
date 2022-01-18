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
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;

@JsonInclude(value = Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class MatchFagsak {

    @JsonProperty(value = "ytelseType", required = true)
    @Valid
    @NotNull
    private FagsakYtelseType ytelseType;

    @JsonProperty(value = "periode", required = false)
    @Valid
    private Periode periode;

    @JsonProperty(value = "bruker", required = false)
    @Valid
    private PersonIdent brukerIdent;

    @JsonInclude(value = Include.NON_ABSENT)
    @JsonProperty(value = "pleietrengendeIdenter", required = false)
    @Size(max = 20)
    @Valid
    private List<PersonIdent> pleietrengendeIdenter;

    @JsonInclude(value = Include.NON_ABSENT)
    @JsonProperty(value = "relatertPersonIdenter", required = false)
    @Size(max = 20)
    @Valid
    private List<PersonIdent> relatertPersonIdenter;

    protected MatchFagsak() {
        //
    }

    public MatchFagsak(@Valid @NotNull FagsakYtelseType ytelseType,
                       @Valid Periode gyldigPeriode,
                       @Valid PersonIdent brukerIdent,
                       @Valid List<PersonIdent> pleietrengendeIdenter,
                       @Valid List<PersonIdent> relatertPersonIdenter) {
        this.ytelseType = ytelseType;
        this.periode = gyldigPeriode;
        this.brukerIdent = brukerIdent;
        this.pleietrengendeIdenter = pleietrengendeIdenter;
        this.relatertPersonIdenter = relatertPersonIdenter;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public Periode getPeriode() {
        return periode;
    }

    public PersonIdent getBruker() {
        return brukerIdent;
    }

    public List<PersonIdent> getPleietrengendeIdenter() {
        return pleietrengendeIdenter;
    }

    public List<PersonIdent> getRelatertPersonIdenter() {
        return relatertPersonIdenter;
    }

}
