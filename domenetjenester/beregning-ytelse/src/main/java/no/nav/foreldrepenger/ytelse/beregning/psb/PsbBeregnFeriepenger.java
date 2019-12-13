package no.nav.foreldrepenger.ytelse.beregning.psb;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import javax.enterprise.context.ApplicationScoped;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PsbBeregnFeriepenger extends BeregnFeriepengerTjeneste {

    // FIXME K9 Hvordan håndterer vi dete for PSB.

    public PsbBeregnFeriepenger() {
        super(60);
    }
}
