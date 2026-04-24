package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;

import java.time.LocalDate;

/**
 * Saksbehandlers vurdering av brukers bosted for én periode.
 * Brukes som felles undertype i {@link BostedAvklaringPeriodeDto} og {@link FastsettBostedPeriodeDto}.
 * <p>
 * Dersom {@code borITrondheimIHelePerioden} er {@code true}, er bruker bosatt i Trondheim hele perioden.
 * Dersom {@code borITrondheimIHelePerioden} er {@code false} og {@code fraflyttingsDato} er satt og etter
 * periodens fom-dato, deles perioden: fra fom bosatt, fra fraflyttingsDato ikke bosatt.
 * Dersom {@code fraflyttingsDato} er null eller ≤ fom, er bruker aldri bosatt i perioden.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BostedVurderingDto {

    @JsonProperty("borITrondheimIHelePerioden")
    @NotNull
    private Boolean borITrondheimIHelePerioden;

    @JsonProperty("fraflyttingsDato")
    private LocalDate fraflyttingsDato;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public BostedVurderingDto() {
        // for jackson
    }

    public BostedVurderingDto(Boolean borITrondheimIHelePerioden, LocalDate fraflyttingsDato, String begrunnelse) {
        this.borITrondheimIHelePerioden = borITrondheimIHelePerioden;
        this.fraflyttingsDato = fraflyttingsDato;
        this.begrunnelse = begrunnelse;
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
