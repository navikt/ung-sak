package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped

//for grenser, se https://www.nav.no/no/nav-og-samfunn/kontakt-nav/utbetalinger/snarveier/ferie-og-feriepenger#chapter-10

public class OmsorgspengerBeregnFeriepenger extends BeregnFeriepengerTjeneste {


    @Override
    public int antallDagerFeriepenger() {
        return 48;
    }

    @Override
    public boolean feriepengeopptjeningForHelg() {
        return true;
    }

    @Override
    public boolean ubegrensedeDagerVedRefusjon() {
        return true;
    }

}
