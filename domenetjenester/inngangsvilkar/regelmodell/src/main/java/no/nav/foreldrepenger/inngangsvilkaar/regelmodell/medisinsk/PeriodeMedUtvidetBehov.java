package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import java.time.LocalDate;

public class PeriodeMedUtvidetBehov extends PleiePeriode {
    public PeriodeMedUtvidetBehov(LocalDate fom, LocalDate tilOgMed) {
        super(fom, tilOgMed, Pleiegrad.UTVIDET_KONTINUERLIG_TILSYN);
    }
}
