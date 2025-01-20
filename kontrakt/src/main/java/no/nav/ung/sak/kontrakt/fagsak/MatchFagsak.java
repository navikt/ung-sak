package no.nav.ung.sak.kontrakt.fagsak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;

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

    protected MatchFagsak() {
        //
    }

    public MatchFagsak(@Valid @NotNull FagsakYtelseType ytelseType,
                       @Valid Periode gyldigPeriode,
                       @Valid PersonIdent brukerIdent) {
        this.ytelseType = ytelseType;
        this.periode = gyldigPeriode;
        this.brukerIdent = brukerIdent;
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

}
