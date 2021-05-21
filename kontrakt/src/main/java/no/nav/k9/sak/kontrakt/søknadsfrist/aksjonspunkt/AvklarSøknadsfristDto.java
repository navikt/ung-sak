package no.nav.k9.sak.kontrakt.søknadsfrist.aksjonspunkt;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE)
public class AvklarSøknadsfristDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Size
    @Valid
    @JsonProperty("avklarteDokumenter")
    private Set<AvklartDokument> avklarteDokumenter;

    @JsonCreator
    public AvklarSøknadsfristDto(@Size(max = 4000)
                                 @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
                                 @JsonProperty("begrunnelse") String begrunnelse,
                                 @NotNull
                                 @Size
                                 @Valid
                                 @JsonProperty(value = "avklarteDokumenter", required = true) Set<AvklartDokument> avklarteDokumenter) {
        super(begrunnelse);
        this.avklarteDokumenter = avklarteDokumenter;
    }

    public Set<AvklartDokument> getAvklarteDokumenter() {
        return avklarteDokumenter;
    }
}
