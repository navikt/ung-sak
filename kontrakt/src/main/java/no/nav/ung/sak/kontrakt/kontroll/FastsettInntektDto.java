package no.nav.ung.sak.kontrakt.kontroll;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_INNTEKT_KODE)
public class FastsettInntektDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "perioder")
    @Size(min = 1, max = 12)
    @Valid
    private List<FastsettInntektPeriodeDto> perioder;

    public FastsettInntektDto() {
        // For Jackson
    }

    public FastsettInntektDto(String begrunnelse, List<FastsettInntektPeriodeDto> perioder) { // NOSONAR
        super(begrunnelse);
        this.perioder = perioder;
    }

    public List<FastsettInntektPeriodeDto> getPerioder() {
        return perioder;
    }
}
