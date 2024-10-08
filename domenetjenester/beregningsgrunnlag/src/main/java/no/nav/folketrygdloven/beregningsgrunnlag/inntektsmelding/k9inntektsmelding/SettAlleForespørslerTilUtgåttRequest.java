package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.k9inntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SettAlleForespørslerTilUtgåttRequest(@NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
