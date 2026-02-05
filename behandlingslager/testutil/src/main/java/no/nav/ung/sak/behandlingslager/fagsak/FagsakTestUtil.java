package no.nav.ung.sak.behandlingslager.fagsak;

import no.nav.ung.sak.tid.DatoIntervallEntitet;

public class FagsakTestUtil {

    public static void oppdaterPeriode(Fagsak f, DatoIntervallEntitet periode) {
        f.setPeriode(periode.getFomDato(), periode.getTomDato());
    }
}
