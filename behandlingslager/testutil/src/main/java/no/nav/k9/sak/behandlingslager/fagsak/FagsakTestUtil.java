package no.nav.k9.sak.behandlingslager.fagsak;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class FagsakTestUtil {

    public static void oppdaterPeriode(Fagsak f, DatoIntervallEntitet periode) {
        f.setPeriode(periode.getFomDato(), periode.getTomDato());
    }
}
