package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

/**
 * Saksbehandlers fastsetting av bosted for én vilkårsperiode etter mottatt uttalelse fra bruker.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FastsettBostedPeriodeDto {

    @JsonProperty("periode")
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty("foreslåttVurderingErGyldig")
    @NotNull
    private Boolean foreslåttVurderingErGyldig;

    /** Ny vurdering brukes kun dersom {@code foreslåttVurderingErGyldig == false}. */
    @JsonProperty("nyErBosattITrondheim")
    private Boolean nyErBosattITrondheim;

    public FastsettBostedPeriodeDto() {
        // for jackson
    }

    public FastsettBostedPeriodeDto(Periode periode, Boolean foreslåttVurderingErGyldig, Boolean nyErBosattITrondheim) {
        this.periode = periode;
        this.foreslåttVurderingErGyldig = foreslåttVurderingErGyldig;
        this.nyErBosattITrondheim = nyErBosattITrondheim;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Boolean getForeslåttVurderingErGyldig() {
        return foreslåttVurderingErGyldig;
    }

    public Boolean getNyErBosattITrondheim() {
        return nyErBosattITrondheim;
    }
}
