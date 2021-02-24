package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;

public interface UtvidetRettKlient {
    void innvilget(UtvidetRett innvilget);
    void avslått(UtvidetRett avslått);
}
