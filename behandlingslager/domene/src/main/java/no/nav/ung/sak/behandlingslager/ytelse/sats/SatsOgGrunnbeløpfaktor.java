package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

import java.math.BigDecimal;

public record SatsOgGrunnbeløpfaktor(UngdomsytelseSatsType satstype, BigDecimal grunnbeløpFaktor){}
