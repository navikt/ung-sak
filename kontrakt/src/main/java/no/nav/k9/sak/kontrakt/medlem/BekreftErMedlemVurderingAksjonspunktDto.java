package no.nav.k9.sak.kontrakt.medlem;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BekreftErMedlemVurderingAksjonspunktDto {

    @JsonProperty(value = "manuellVurderingTypeKode")
    @Valid
    private MedlemskapManuellVurderingType manuellVurderingTypeKode;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    public BekreftErMedlemVurderingAksjonspunktDto(MedlemskapManuellVurderingType manuellVurderingTypeKode, String begrunnelse) {
        this.manuellVurderingTypeKode = manuellVurderingTypeKode;
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public MedlemskapManuellVurderingType getManuellVurderingTypeKode() {
        return manuellVurderingTypeKode;
    }
}
