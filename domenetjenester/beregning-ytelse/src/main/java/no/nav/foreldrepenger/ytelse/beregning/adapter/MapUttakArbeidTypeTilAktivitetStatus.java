package no.nav.foreldrepenger.ytelse.beregning.adapter;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;

public class MapUttakArbeidTypeTilAktivitetStatus {
    private MapUttakArbeidTypeTilAktivitetStatus() {
        // private constructor
    }

    public static AktivitetStatus map(UttakArbeidType uttakArbeidType) {
        if (uttakArbeidType.erArbeidstakerEllerFrilans()) {
            return AktivitetStatus.ATFL;
        } else if (uttakArbeidType.equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return AktivitetStatus.SN;
        } else if (uttakArbeidType.equals(UttakArbeidType.ANNET)) {
            return AktivitetStatus.ANNET;
        }
        throw new IllegalArgumentException("UttakArbeidType er ukjent!" + uttakArbeidType);
    }
}
