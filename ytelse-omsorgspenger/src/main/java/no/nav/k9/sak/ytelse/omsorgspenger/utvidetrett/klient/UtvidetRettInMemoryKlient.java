package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

@RequestScoped
@Alternative
public class UtvidetRettInMemoryKlient implements UtvidetRettKlient {

    private UtvidetRettResultat resultat;

    @Override
    public UtvidetRettResultat hentResultat(FagsakYtelseType ytelseType, Saksnummer saksnummer, UUID behandlingUUID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void innvilget(FagsakYtelseType ytelseType, UUID behandlingUUID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void avslått(FagsakYtelseType ytelseType, UUID behandlingUUID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void forkast(FagsakYtelseType ytelseType, UUID behandlingUUID) {
        // TODO Auto-generated method stub

    }

    @Override
    public void oppgaveLøst(FagsakYtelseType ytelseType, UUID behandlingUUID, Oppgaver oppgaver) {
        // TODO Auto-generated method stub

    }

}
