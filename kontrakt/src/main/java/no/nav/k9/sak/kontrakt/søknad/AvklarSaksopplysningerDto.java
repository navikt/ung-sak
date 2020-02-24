package no.nav.k9.sak.kontrakt.søknad;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS_KODE)
public class AvklarSaksopplysningerDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "fortsettBehandling")
    private boolean fortsettBehandling;

    /** {@link PersonstatusType#getKode()}. */
    @JsonProperty(value = "personstatus", required = true)
    @NotNull
    @Size(min = 1, max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N}_\\.\\-/]+$")
    private String personstatus;

    public AvklarSaksopplysningerDto() {
        //
    }

    public AvklarSaksopplysningerDto(String begrunnelse, PersonstatusType personstatus,
                                     boolean fortsettBehandling) {
        super(begrunnelse);
        this.personstatus = Objects.requireNonNull(personstatus, "personstatus").getKode();
        this.fortsettBehandling = fortsettBehandling;
    }

    public PersonstatusType getPersonstatus() {
        return PersonstatusType.fraKode(personstatus);
    }

    public boolean isFortsettBehandling() {
        return fortsettBehandling;
    }

}
