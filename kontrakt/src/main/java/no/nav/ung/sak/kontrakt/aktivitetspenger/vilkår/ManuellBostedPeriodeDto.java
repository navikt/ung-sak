package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;

import java.time.LocalDate;

/**
 * Én periode i {@link ManuellVurderingBostedsvilkårDto}.
 * {@code fom} identifiserer perioden (= skjæringstidspunktet).
 * {@code begrunnelse} er fritekstvurdering for IKKE_OPPFYLT-delen av perioden.
 */
public class ManuellBostedPeriodeDto {

    @JsonProperty("fom")
    @NotNull
    private LocalDate fom;

    @JsonProperty("begrunnelse")
    @NotNull
    @Size(min = 1, max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public ManuellBostedPeriodeDto() {
        // for Jackson
    }

    @JsonCreator
    public ManuellBostedPeriodeDto(@JsonProperty("fom") LocalDate fom,
                                    @JsonProperty("begrunnelse") String begrunnelse) {
        this.fom = fom;
        this.begrunnelse = begrunnelse;
    }

    public LocalDate getFom() {
        return fom;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
