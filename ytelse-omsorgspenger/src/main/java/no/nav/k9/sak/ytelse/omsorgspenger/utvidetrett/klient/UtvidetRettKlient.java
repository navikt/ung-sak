package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient;

import java.util.UUID;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.Saksnummer;

public interface UtvidetRettKlient {

    UtvidetRettResultat hentResultat(FagsakYtelseType ytelseType, Saksnummer saksnummer, UUID behandlingUUID);

    void innvilget(FagsakYtelseType ytelseType, UUID behandlingUUID);

    void avslått(FagsakYtelseType ytelseType, UUID behandlingUUID);

    void forkast(FagsakYtelseType ytelseType, UUID behandlingUUID);

    void oppgaveLøst(FagsakYtelseType ytelseType, UUID behandlingUUID, Oppgaver oppgaver);

}
