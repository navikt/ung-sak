package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;

@RequestScoped
@Alternative
public class UtvidetRettInMemoryKlient implements UtvidetRettKlient {

    @Override
    public void innvilget(FagsakYtelseType ytelseType, UUID behandlingUUID, UtvidetRett utvidetRett) {
        // no-op
    }

}
