package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Resultat;


/**
 * Ett element i oversiktsvisningen til sykdomssteget.
 *
 * Én gitt vurdering blir vist gjennom mange instanser av denne klassen. Det blir en ny instans når vurderingen:
 *
 * 1. ...gjelder for forskjellige perioder (enten gjennom egen periodeliste eller fordi deler av
 *    resultatet er overskrevet av en annen vurdering).
 * 2. ...dekker kun deler av søknadsperiodene til en eller flere søkere.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingOversiktElement {

    /**
     * IDen til SykdomVurdering (og ikke til en gitt SykdomVurderingVersjon).
     */
    @JsonProperty(value = "id")
    @Valid
    private String id;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "gjelderForSøker")
    @Valid
    private boolean gjelderForSøker;

    @JsonProperty(value = "gjelderForAnnenPart")
    @Valid
    private boolean gjelderForAnnenPart;


    public SykdomVurderingOversiktElement(String id, Resultat resultat, Periode periode,
            boolean gjelderForSøker, boolean gjelderForAnnenPart) {
        this.id = id;
        this.resultat = resultat;
        this.periode = periode;
        this.gjelderForSøker = gjelderForSøker;
        this.gjelderForAnnenPart = gjelderForAnnenPart;
    }


    public String getId() {
        return id;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public Periode getPeriode() {
        return periode;
    }

    public boolean isGjelderForSøker() {
        return gjelderForSøker;
    }

    public boolean isGjelderForAnnenPart() {
        return gjelderForAnnenPart;
    }
}
