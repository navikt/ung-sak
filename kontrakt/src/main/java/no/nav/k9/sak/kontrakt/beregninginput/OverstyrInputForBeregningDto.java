package no.nav.k9.sak.kontrakt.beregninginput;

import java.util.List;

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
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.KompletthetsPeriode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYR_BEREGNING_INPUT)
public class OverstyrInputForBeregningDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1)
    @JsonProperty("perioder")
    private List<OverstyrBeregningInputPeriode> perioder;

    public OverstyrInputForBeregningDto() {
    }

    @JsonCreator
    public OverstyrInputForBeregningDto(@JsonProperty("begrunnelse") @Size(max = 4000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse,
                                        @Valid @JsonProperty(value = "perioder", required = true) @Size(min = 1) List<OverstyrBeregningInputPeriode> perioder) {
        super(begrunnelse);
        this.perioder = perioder;
    }

    public List<OverstyrBeregningInputPeriode> getPerioder() {
        return perioder;
    }
}
