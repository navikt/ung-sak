package no.nav.ung.sak.kontrakt.kontroll;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.ung.sak.typer.Periode;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FastsettInntektPeriodeDto {

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "fastsattInnntekt")
    @Min(0)
    @Max(1000000)
    private Integer fastsattInnntekt;

    @JsonProperty(value = "valg", required = true)
    @NotNull
    @Valid
    private BrukKontrollertInntektValg valg;

    public FastsettInntektPeriodeDto() {
        // For Jackson
    }

    public FastsettInntektPeriodeDto(Periode periode, Integer fastsattInnntekt, BrukKontrollertInntektValg valg) { // NOSONAR
        this.periode = periode;
        this.fastsattInnntekt = fastsattInnntekt;
        this.valg = valg;
    }

    public Periode getPeriode() {
        return periode;
    }


    public Integer getFastsattInnntekt() {
        return fastsattInnntekt;
    }

    public BrukKontrollertInntektValg getValg() {
        return valg;
    }

    @AssertTrue(message = "Må ha satt inntekt for valg MANUELT_FASTSATT")
    public boolean isHarInntektForManueltFastsatt() {
        return !valg.equals(BrukKontrollertInntektValg.MANUELT_FASTSATT) || fastsattInnntekt != null;
    }

}
