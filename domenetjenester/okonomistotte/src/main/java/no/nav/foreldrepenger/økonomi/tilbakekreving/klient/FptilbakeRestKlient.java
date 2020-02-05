package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import no.nav.k9.sak.typer.Saksnummer;

public interface FptilbakeRestKlient {

    boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer);
}
