package no.nav.k9.sak.domene.arbeidsgiver;


import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.typer.Arbeidsgiver;

public interface ArbeidsgiverTjeneste {

    ArbeidsgiverOpplysninger hent(Arbeidsgiver arbeidsgiver);

    Virksomhet hentVirksomhet(String orgnr);

    Arbeidsgiver hentArbeidsgiver(String orgnr, String arbeidsgiverIdentifikator);
}
