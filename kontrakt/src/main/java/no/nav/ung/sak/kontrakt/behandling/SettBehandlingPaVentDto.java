package no.nav.ung.sak.kontrakt.behandling;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SettBehandlingPaVentDto {

    @JsonProperty(value = "behandlingId")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @JsonAlias("versjon")
    @JsonProperty(value = "behandlingVersjon")
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    @JsonProperty(value = "frist")
    private LocalDate frist;

    @JsonProperty(value = "ventearsak")
    private Venteårsak ventearsak;

    @JsonProperty(value = "ventearsakVariant")
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Size(max = 200)
    private String ventearsakVariant;

    public SettBehandlingPaVentDto() {
        //
    }

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public LocalDate getFrist() {
        return frist;
    }

    public Venteårsak getVentearsak() {
        return ventearsak;
    }

    public String getVentearsakVariant() {
        return ventearsakVariant;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    public void setFrist(LocalDate frist) {
        this.frist = frist;
    }

    public void setVentearsak(Venteårsak ventearsak) {
        this.ventearsak = ventearsak;
    }

}
