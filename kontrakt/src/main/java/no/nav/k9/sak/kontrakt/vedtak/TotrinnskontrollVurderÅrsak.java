package no.nav.k9.sak.kontrakt.vedtak;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;


/** @deprecated Bruk {@link VurderÅrsak} i stedet for dette. */
@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TotrinnskontrollVurderÅrsak {

    /** @deprecated Bruk {@link VurderÅrsak} i stedet for denne klassen. */
    @Deprecated
    @JsonProperty(value = "kode", required = true)
    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String kode;

    /** @deprecated Bruk {@link VurderÅrsak} i stedet for denne klassen. */
    @Deprecated
    @JsonProperty(value = "navn", required = true)
    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}\\p{P}\\p{M}\\p{Sc}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String navn;

    public TotrinnskontrollVurderÅrsak(VurderÅrsak vurderÅrsak) {
        this.kode = vurderÅrsak.getKode();
        this.navn = vurderÅrsak.getNavn();
    }

    public String getKode() {
        return kode;
    }

    public String getNavn() {
        return navn;
    }
}
