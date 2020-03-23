package no.nav.k9.sak.økonomi.tilbakekreving.klient;

import no.nav.k9.sak.typer.Saksnummer;

public interface FptilbakeRestKlient {

    boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer);
}
