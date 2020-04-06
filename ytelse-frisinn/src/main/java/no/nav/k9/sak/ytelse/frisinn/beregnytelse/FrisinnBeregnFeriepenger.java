package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;

import javax.enterprise.context.ApplicationScoped;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnBeregnFeriepenger extends BeregnFeriepengerTjeneste {

    // FIXME K9 Hvordan håndterer vi dete for PSB.

    public FrisinnBeregnFeriepenger() {
        super(60);
    }
}
