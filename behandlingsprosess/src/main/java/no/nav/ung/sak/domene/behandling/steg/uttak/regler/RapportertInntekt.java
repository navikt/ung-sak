package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import java.math.BigDecimal;

public record RapportertInntekt (
    InntektType inntektType,
    BigDecimal beløp){}
