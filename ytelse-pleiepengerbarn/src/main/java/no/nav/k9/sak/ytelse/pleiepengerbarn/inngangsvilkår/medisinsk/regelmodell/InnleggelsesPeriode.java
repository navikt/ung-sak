package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk.regelmodell;

import java.time.LocalDate;

public class InnleggelsesPeriode extends PleiePeriode {
    public InnleggelsesPeriode(LocalDate fom, LocalDate tilOgMed) {
        super(fom, tilOgMed, Pleiegrad.INNLEGGELSE);
    }
}
