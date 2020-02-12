package no.nav.k9.sak.kontrakt.arbeidsforhold;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class NaringsvirksomhetTypeDto {

    @JsonAlias("annen")
    @JsonProperty(value = "ANNEN")
    private boolean annen;

    @JsonAlias("dagmammaEllerFamiliebarnehage")
    @JsonProperty(value = "DAGMAMMA")
    private boolean dagmammaEllerFamiliebarnehage;

    @JsonAlias("fiske")
    @JsonProperty(value = "FISKE")
    private boolean fiske;

    @JsonAlias("jordbrukEllerSkogbruk")
    @JsonProperty(value = "JORDBRUK_SKOGBRUK")
    private boolean jordbrukEllerSkogbruk;

    @JsonProperty("typeFiske")
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String typeFiske;

    public boolean getAnnen() {
        return annen;
    }

    public boolean getDagmammaEllerFamiliebarnehage() {
        return dagmammaEllerFamiliebarnehage;
    }

    public boolean getFiske() {
        return fiske;
    }

    public boolean getJordbrukEllerSkogbruk() {
        return jordbrukEllerSkogbruk;
    }

    public String getTypeFiske() {
        return typeFiske;
    }

    public void setAnnen(boolean annen) {
        this.annen = annen;
    }

    public void setDagmammaEllerFamiliebarnehage(boolean dagmammaEllerFamiliebarnehage) {
        this.dagmammaEllerFamiliebarnehage = dagmammaEllerFamiliebarnehage;
    }

    public void setFiske(boolean fiske) {
        this.fiske = fiske;
    }

    public void setJordbrukEllerSkogbruk(boolean jordbrukEllerSkogbruk) {
        this.jordbrukEllerSkogbruk = jordbrukEllerSkogbruk;
    }

    public void setTypeFiske(String typeFiske) {
        this.typeFiske = typeFiske;
    }
}
