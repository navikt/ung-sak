package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

public interface FptilbakeRestKlient {

    boolean harÅpenTilbakekrevingsbehandling(Saksnummer saksnummer);
}
