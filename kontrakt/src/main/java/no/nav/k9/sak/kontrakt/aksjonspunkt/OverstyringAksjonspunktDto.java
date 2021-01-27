package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.sak.typer.Periode;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
public abstract class OverstyringAksjonspunktDto implements AksjonspunktKode, OverstyringAksjonspunkt {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    protected OverstyringAksjonspunktDto() {
        // default ctor
    }

    protected OverstyringAksjonspunktDto(Periode periode, String begrunnelse) {
        this.periode = periode; // NOSONAR
        this.begrunnelse = begrunnelse;
    }

    public OverstyringAksjonspunktDto(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public Periode getPeriode() {
        return periode;
    }

    @Override
    public String getKode() {
        if (this.getClass().isAnnotationPresent(JsonTypeName.class)) {
            return this.getClass().getDeclaredAnnotation(JsonTypeName.class).value();
        }
        throw new IllegalStateException("Utvikler-feil:" + this.getClass().getSimpleName() + " er uten JsonTypeName annotation.");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var other = (OverstyringAksjonspunktDto) obj;
        return Objects.equals(getKode(), other.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKode());
    }

    @Override
    public String toString() {
        return getClass() + "<kode=" + getKode() + ", begrunnelse=" + getBegrunnelse() + ", periode=" + getPeriode() + ">";
    }
}
