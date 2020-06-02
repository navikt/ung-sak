package no.nav.k9.sak.kontrakt.vedtak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TotrinnskontrollSkjermlenkeContextDto {

    @JsonProperty(value = "skjermlenkeType", required = true)
    @NotNull
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}\\s\\-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String skjermlenkeType;

    @JsonProperty(value = "totrinnskontrollAksjonspunkter")
    @Size(max = 50)
    @Valid
    private List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter = new ArrayList<>();

    public TotrinnskontrollSkjermlenkeContextDto() {
        //
    }

    public TotrinnskontrollSkjermlenkeContextDto(SkjermlenkeType skjermlenkeType, List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter) {
        this.totrinnskontrollAksjonspunkter = totrinnskontrollAksjonspunkter;
        this.skjermlenkeType = skjermlenkeType.getKode();
    }

    public String getSkjermlenkeType() {
        return skjermlenkeType;
    }

    public List<TotrinnskontrollAksjonspunkterDto> getTotrinnskontrollAksjonspunkter() {
        return Collections.unmodifiableList(totrinnskontrollAksjonspunkter);
    }

    public void setSkjermlenkeType(String skjermlenkeType) {
        this.skjermlenkeType = skjermlenkeType;
    }

    public void setTotrinnskontrollAksjonspunkter(List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter) {
        this.totrinnskontrollAksjonspunkter = totrinnskontrollAksjonspunkter;
    }
}
