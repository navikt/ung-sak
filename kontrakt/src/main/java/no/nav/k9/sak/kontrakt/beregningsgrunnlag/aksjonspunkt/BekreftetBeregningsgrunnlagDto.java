package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.Objects;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktKode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
public abstract class BekreftetBeregningsgrunnlagDto implements AksjonspunktKode {

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    protected BekreftetBeregningsgrunnlagDto() {
        // For Jackson
    }

    protected BekreftetBeregningsgrunnlagDto(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @AbacAttributt("aksjonspunktKode")
    @Override
    public String getKode() {
        if (this.getClass().isAnnotationPresent(JsonTypeName.class)) {
            return this.getClass().getDeclaredAnnotation(JsonTypeName.class).value();
        }
        throw new IllegalStateException("Utvikler-feil:" + this.getClass().getSimpleName() + " er uten JsonTypeName annotation.");
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (BekreftetBeregningsgrunnlagDto) obj;
        return Objects.equals(getKode(), other.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKode());
    }

    @Override
    public String toString() {
        return getClass() + "<kode=" + getKode() + ", begrunnelse=" + getBegrunnelse() + ">";
    }
}
