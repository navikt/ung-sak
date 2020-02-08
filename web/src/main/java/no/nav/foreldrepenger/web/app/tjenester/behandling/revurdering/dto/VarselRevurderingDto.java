package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.dto;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public abstract class VarselRevurderingDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "sendVarsel", required = true)
    private boolean sendVarsel;

    @JsonProperty(value = "fritekst", required = true)
    @NotNull
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fritekst;

    @JsonProperty(value = "frist")
    private LocalDate frist;

    @JsonProperty(value = "ventearsak")
    @Valid
    private Venteårsak ventearsak;

    public VarselRevurderingDto(String begrunnelse,
                                boolean sendVarsel,
                                String fritekst,
                                LocalDate frist,
                                Venteårsak ventearsak) {
        super(begrunnelse);
        this.sendVarsel = sendVarsel;
        this.fritekst = fritekst;
        this.frist = frist;
        this.ventearsak = ventearsak;
    }

    protected VarselRevurderingDto() {
        //
    }

    public boolean isSendVarsel() {
        return sendVarsel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public LocalDate getFrist() {
        return frist;
    }

    public Venteårsak getVentearsak() {
        return ventearsak;
    }
}
