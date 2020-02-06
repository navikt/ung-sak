package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import java.time.LocalDate;

public class InnleggelsesPeriode extends PleiePeriode {
    public InnleggelsesPeriode(LocalDate fom, LocalDate tilOgMed) {
        super(fom, tilOgMed, Pleiegrad.INNLEGGELSE);
    }
}
