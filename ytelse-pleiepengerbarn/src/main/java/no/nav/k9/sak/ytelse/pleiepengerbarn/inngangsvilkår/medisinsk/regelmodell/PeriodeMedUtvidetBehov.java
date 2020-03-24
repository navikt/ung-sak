package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;

public class PeriodeMedUtvidetBehov extends PleiePeriode {
    public PeriodeMedUtvidetBehov(LocalDate fom, LocalDate tilOgMed) {
        super(fom, tilOgMed, Pleiegrad.UTVIDET_KONTINUERLIG_TILSYN);
    }
}
