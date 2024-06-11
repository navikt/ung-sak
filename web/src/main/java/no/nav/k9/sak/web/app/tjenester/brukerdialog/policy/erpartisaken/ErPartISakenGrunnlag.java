package no.nav.k9.sak.web.app.tjenester.brukerdialog.policy.erpartisaken;

import no.nav.k9.sak.typer.AktørId;

import java.util.List;

public record ErPartISakenGrunnlag(List<AktørId> parterISaken) {
}
