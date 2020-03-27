package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;

import javax.enterprise.context.ApplicationScoped;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class OmsorgspengerBeregnFeriepenger extends BeregnFeriepengerTjeneste {

    // FIXME K9 Hvordan håndterer vi dete for PSB.

    public OmsorgspengerBeregnFeriepenger() {
        super(60);
    }
}
