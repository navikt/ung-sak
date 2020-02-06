package no.nav.k9.sak.kontrakt.aksjonspunkt;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
public abstract class BekreftetAksjonspunktDto implements AksjonspunktKode {

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    protected BekreftetAksjonspunktDto() {
        // For Jackson
    }

    protected BekreftetAksjonspunktDto(String begrunnelse) {
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
        throw new IllegalStateException("Utvikler-feil:" +this.getClass().getSimpleName() +" er uten JsonTypeName annotation.");
    }
}
