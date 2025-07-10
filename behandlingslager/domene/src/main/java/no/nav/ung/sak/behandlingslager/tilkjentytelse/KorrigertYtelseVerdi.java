package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import no.nav.ung.kodeverk.ytelse.KorrigertYtelseÅrsak;

import java.math.BigDecimal;

public record KorrigertYtelseVerdi(BigDecimal dagsats,
                                   KorrigertYtelseÅrsak årsak) {
}
