package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerBeregnFeriepenger extends BeregnFeriepengerTjeneste {

    public OmsorgspengerBeregnFeriepenger() {
        //for grense, se https://www.nav.no/no/nav-og-samfunn/kontakt-nav/utbetalinger/snarveier/ferie-og-feriepenger#chapter-10
        super(48, true, true);
    }
}
