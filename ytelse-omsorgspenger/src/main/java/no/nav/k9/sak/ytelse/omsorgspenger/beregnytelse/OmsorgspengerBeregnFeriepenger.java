package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerBeregnFeriepenger extends BeregnFeriepengerTjeneste {

    public OmsorgspengerBeregnFeriepenger() {
        //for grense, se https://www.nav.no/no/nav-og-samfunn/kontakt-nav/utbetalinger/snarveier/ferie-og-feriepenger#chapter-10
        super(48, true, true);
    }
}
