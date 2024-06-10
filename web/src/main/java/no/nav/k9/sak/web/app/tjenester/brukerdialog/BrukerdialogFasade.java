package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.typer.AktørId;

public interface BrukerdialogFasade<T extends BrukerdialogTilgangEvaluation>  {
    T harGyldigOmsorgsdagerVedtak(AktørId pleietrengendeAktørId);
}
