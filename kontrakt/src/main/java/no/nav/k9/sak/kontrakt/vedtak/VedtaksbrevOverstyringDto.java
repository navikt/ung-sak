package no.nav.k9.sak.kontrakt.vedtak;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public abstract class VedtaksbrevOverstyringDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value="overskrift")
    @Size(max = 200)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String overskrift;

    @JsonProperty(value="fritekstBrev")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fritekstBrev;

    @JsonProperty(value="skalBrukeOverstyrendeFritekstBrev", required=true)
    private boolean skalBrukeOverstyrendeFritekstBrev;

    protected VedtaksbrevOverstyringDto() {
        // For Jackson
    }

    protected VedtaksbrevOverstyringDto(String begrunnelse, String overskrift, String fritekstBrev,
                              boolean skalBrukeOverstyrendeFritekstBrev) {
        super(begrunnelse);
        this.overskrift = overskrift;
        this.fritekstBrev = fritekstBrev;
        this.skalBrukeOverstyrendeFritekstBrev = skalBrukeOverstyrendeFritekstBrev;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public String getFritekstBrev() {
        return fritekstBrev;
    }

    public boolean isSkalBrukeOverstyrendeFritekstBrev() {
        return skalBrukeOverstyrendeFritekstBrev;
    }
}
