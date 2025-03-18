package no.nav.ung.sak.kontrakt.kontroll;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_INNTEKT_KODE)
public class ManueltFastsattInntektDto {

    @JsonProperty(value = "arbeidsinntekt")
    @Min(0)
    @Max(1000000)
    private Integer arbeidsinntekt;

    @JsonProperty(value = "ytelse")
    @Min(0)
    @Max(1000000)
    private Integer ytelse;

    public ManueltFastsattInntektDto() {
    }

    public ManueltFastsattInntektDto(Integer arbeidsinntekt, Integer ytelse) {
        this.arbeidsinntekt = arbeidsinntekt;
        this.ytelse = ytelse;
    }


    public Integer getArbeidsinntekt() {
        return arbeidsinntekt;
    }

    public Integer getYtelse() {
        return ytelse;
    }

    @AssertTrue
    public boolean getErMinstEnVerdiSatt() {
        return arbeidsinntekt != null || ytelse != null;
    }

}
