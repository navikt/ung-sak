package no.nav.k9.sak.kontrakt.uttak;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYR_UTTAKSGRAD)
public class OverstyrUttaksgradDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1)
    @JsonProperty("uttaksgradPerioder")
    private List<OverstyrUttakUtbetalingsgradPeriode> uttaksgradPerioder;

    public OverstyrUttaksgradDto() {
    }

    @JsonCreator
    public OverstyrUttaksgradDto(@JsonProperty("begrunnelse") @Size(max = 4000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse,
                                 @Valid @JsonProperty(value = "uttaksgradPerioder", required = true) @Size(min = 1) List<OverstyrUttakUtbetalingsgradPeriode> uttaksgradPerioder) {
        super(begrunnelse);
        this.uttaksgradPerioder = uttaksgradPerioder;
    }

    public List<OverstyrUttakUtbetalingsgradPeriode> getUttaksgradPerioder() {
        return uttaksgradPerioder;
    }
}
