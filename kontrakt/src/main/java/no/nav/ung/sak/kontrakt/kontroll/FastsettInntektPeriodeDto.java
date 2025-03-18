package no.nav.ung.sak.kontrakt.kontroll;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsettInntektPeriodeDto {

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "refusjon", required = false)
    @Valid
    private ManueltFastsattInntektDto inntekt;

    @JsonProperty(value = "valg", required = true)
    @NotNull
    @Valid
    private BrukKontrollertInntektValg valg;

    public FastsettInntektPeriodeDto() {
        // For Jackson
    }

    public FastsettInntektPeriodeDto(Periode periode, ManueltFastsattInntektDto inntekt, BrukKontrollertInntektValg valg) { // NOSONAR
        this.periode = periode;
        this.inntekt = inntekt;
        this.valg = valg;
    }

    public Periode getPeriode() {
        return periode;
    }

    public ManueltFastsattInntektDto getInntekt() {
        return inntekt;
    }

    public BrukKontrollertInntektValg getValg() {
        return valg;
    }
}
