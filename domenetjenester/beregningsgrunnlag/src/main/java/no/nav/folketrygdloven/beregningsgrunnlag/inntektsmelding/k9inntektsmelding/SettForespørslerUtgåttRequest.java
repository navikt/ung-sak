package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SettForespørslerUtgåttRequest(@Valid OrganisasjonsnummerDto orgnummer,
                                            @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                            LocalDate skjæringstidspunkt) {
}
