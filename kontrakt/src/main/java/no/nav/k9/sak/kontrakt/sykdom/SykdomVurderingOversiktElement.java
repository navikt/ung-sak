package no.nav.k9.sak.kontrakt.sykdom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.typer.Periode;

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
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
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

    @JsonProperty(value = "endretIDenneBehandlingen")
    @Valid
    private boolean endretIDenneBehandlingen;

    @JsonProperty(value = "erInnleggelsesperiode")
    @Valid
    private boolean erInnleggelsesperiode;
    

    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();


    public SykdomVurderingOversiktElement(String id, Resultat resultat, Periode periode, boolean endretIDenneBehandlingen, List<ResourceLink> links) {
        this.id = id;
        this.resultat = resultat;
        this.periode = periode;
        this.endretIDenneBehandlingen = endretIDenneBehandlingen;
        this.links = new ArrayList<>(links);
    }
    
    public SykdomVurderingOversiktElement(SykdomVurderingOversiktElement element) {
        this.id = element.id;
        this.resultat = element.resultat;
        this.periode = element.periode;
        this.gjelderForSøker = element.gjelderForSøker;
        this.gjelderForAnnenPart = element.gjelderForAnnenPart;
        this.endretIDenneBehandlingen = element.endretIDenneBehandlingen;
        this.erInnleggelsesperiode = element.erInnleggelsesperiode;
        this.links = element.links;
    }
    

    public SykdomVurderingOversiktElement(){

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
    
    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public boolean isGjelderForSøker() {
        return gjelderForSøker;
    }
    
    public void setGjelderForSøker(boolean gjelderForSøker) {
        this.gjelderForSøker = gjelderForSøker;
    }

    public boolean isGjelderForAnnenPart() {
        return gjelderForAnnenPart;
    }
    
    public void setGjelderForAnnenPart(boolean gjelderForAnnenPart) {
        this.gjelderForAnnenPart = gjelderForAnnenPart;
    }
    
    public boolean isEndretIDenneBehandlingen() {
        return endretIDenneBehandlingen;
    }
    
    public boolean isErInnleggelsesperiode() {
        return erInnleggelsesperiode;
    }
    
    public void setErInnleggelsesperiode(boolean erInnleggelsesperiode) {
        this.erInnleggelsesperiode = erInnleggelsesperiode;
    }
    
    public List<ResourceLink> getLinks() {
        return Collections.unmodifiableList(links);
    }
}
