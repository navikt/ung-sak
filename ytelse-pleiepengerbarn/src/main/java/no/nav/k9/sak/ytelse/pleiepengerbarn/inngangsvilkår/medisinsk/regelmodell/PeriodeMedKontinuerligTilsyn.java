package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;

public class PeriodeMedKontinuerligTilsyn extends PleiePeriode {
    public PeriodeMedKontinuerligTilsyn(LocalDate fom, LocalDate tilOgMed) {
        super(fom, tilOgMed, Pleiegrad.KONTINUERLIG_TILSYN);
    }
}
