package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;

import javax.enterprise.context.ApplicationScoped;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PleiepengerBeregnFeriepenger extends BeregnFeriepengerTjeneste {

    // FIXME K9 Hvordan håndterer vi dete for PSB.

    public PleiepengerBeregnFeriepenger() {
        super(60);
    }
}
