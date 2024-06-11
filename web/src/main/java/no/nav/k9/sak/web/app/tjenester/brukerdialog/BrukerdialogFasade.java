package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.BrukerdialogTilgangEvaluation;

public interface BrukerdialogFasade<T extends BrukerdialogTilgangEvaluation>  {
    T harGyldigOmsorgsdagerVedtak(AktørId pleietrengendeAktørId);
}
