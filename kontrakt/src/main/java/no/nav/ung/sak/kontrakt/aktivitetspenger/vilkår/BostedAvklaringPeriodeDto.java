package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.sak.typer.Periode;

/**
 * Saksbehandlers fakta-avklaring for én vilkårsperiode om brukers bosted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BostedAvklaringPeriodeDto {

    @JsonProperty("periode")
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty("vurdering")
    @NotNull
    @Valid
    private BostedVurderingDto vurdering;

    public BostedAvklaringPeriodeDto() {
        // for jackson
    }

    public BostedAvklaringPeriodeDto(Periode periode, BostedVurderingDto vurdering) {
        this.periode = periode;
        this.vurdering = vurdering;
    }

    public Periode getPeriode() {
        return periode;
    }

    public BostedVurderingDto getVurdering() {
        return vurdering;
    }
}
