package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import java.util.UUID;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;

public interface UtvidetRettKlient {

    void innvilget(FagsakYtelseType ytelseType, UUID behandlingUUID, UtvidetRett innvilget);

}
