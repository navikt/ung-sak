package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OppdaterForespørslerISakRequest(@NotNull @Valid AktørIdDto aktørId,
                                              @NotNull @Valid Map<LocalDate, List<OrganisasjonsnummerDto>> skjæringstidspunkterPerOrganisasjon,
                                              @NotNull YtelseType ytelsetype,
                                              @NotNull @Valid SaksnummerDto saksnummer) {
}
