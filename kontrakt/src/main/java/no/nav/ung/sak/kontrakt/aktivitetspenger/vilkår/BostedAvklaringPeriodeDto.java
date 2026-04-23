package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;

/**
 * Saksbehandlers fakta-avklaring for én vilkårsperiode om brukers bosted.
 * <p>
 * Dersom {@code borITrondheimIHelePerioden} er {@code true}, er bruker bosatt i Trondheim hele perioden
 * og det skal kun finnes ett BostedAvklaring-innslag fra starten av perioden.
 * Dersom {@code borITrondheimIHelePerioden} er {@code false} og {@code fraflyttingsDato} er satt
 * og etter {@code periode.fom}, deles perioden:
 * [{@code periode.fom}, {@code fraflyttingsDato} - 1] → bosatt, [{@code fraflyttingsDato}, {@code periode.tom}] → ikke bosatt.
 * Dersom {@code fraflyttingsDato} er satt og ≤ {@code periode.fom}, er bruker aldri bosatt i perioden.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BostedAvklaringPeriodeDto {

    @JsonProperty("periode")
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty("borITrondheimIHelePerioden")
    @NotNull
    private Boolean borITrondheimIHelePerioden;

    @JsonProperty("fraflyttingsDato")
    private LocalDate fraflyttingsDato;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public BostedAvklaringPeriodeDto() {
        // for jackson
    }

    public BostedAvklaringPeriodeDto(Periode periode, Boolean borITrondheimIHelePerioden, LocalDate fraflyttingsDato, String begrunnelse) {
        this.periode = periode;
        this.borITrondheimIHelePerioden = borITrondheimIHelePerioden;
        this.fraflyttingsDato = fraflyttingsDato;
        this.begrunnelse = begrunnelse;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Boolean getBorITrondheimIHelePerioden() {
        return borITrondheimIHelePerioden;
    }

    public LocalDate getFraflyttingsDato() {
        return fraflyttingsDato;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
