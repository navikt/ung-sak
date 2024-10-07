package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull YtelseType ytelsetype,
                                        @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
