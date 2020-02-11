package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import java.time.LocalDate;

public class PeriodeMedKontinuerligTilsyn extends PleiePeriode {
    public PeriodeMedKontinuerligTilsyn(LocalDate fom, LocalDate tilOgMed) {
        super(fom, tilOgMed, Pleiegrad.KONTINUERLIG_TILSYN);
    }
}
