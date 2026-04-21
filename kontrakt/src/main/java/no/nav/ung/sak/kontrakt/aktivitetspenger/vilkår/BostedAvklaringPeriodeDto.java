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

/**
 * Saksbehandlers fakta-avklaring for én vilkårsperiode om brukers bosted.
 * Skjæringstidspunktet er fom-datoen i perioden.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BostedAvklaringPeriodeDto {

    @JsonProperty("periode")
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty("erBosattITrondheim")
    @NotNull
    private Boolean erBosattITrondheim;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public BostedAvklaringPeriodeDto() {
        // for jackson
    }

    public BostedAvklaringPeriodeDto(Periode periode, Boolean erBosattITrondheim, String begrunnelse) {
        this.periode = periode;
        this.erBosattITrondheim = erBosattITrondheim;
        this.begrunnelse = begrunnelse;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Boolean getErBosattITrondheim() {
        return erBosattITrondheim;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
