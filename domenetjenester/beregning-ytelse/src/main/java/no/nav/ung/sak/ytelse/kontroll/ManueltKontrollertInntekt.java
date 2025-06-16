package no.nav.ung.sak.ytelse.kontroll;

import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;

import java.math.BigDecimal;

public record ManueltKontrollertInntekt(KontrollertInntektKilde kilde, BigDecimal samletInntekt, String begrunnelse) {
}
