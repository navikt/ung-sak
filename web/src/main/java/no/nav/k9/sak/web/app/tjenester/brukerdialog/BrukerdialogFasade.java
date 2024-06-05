package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.PolicyEvaluation;

public interface BrukerdialogFasade<T extends PolicyEvaluation>  {
    T harGyldigOmsorgsdagerVedtak(AktørId pleietrengendeAktørId);
}
