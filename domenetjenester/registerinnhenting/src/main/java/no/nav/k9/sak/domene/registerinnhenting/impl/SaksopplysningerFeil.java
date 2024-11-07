package no.nav.k9.sak.domene.registerinnhenting.impl;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface SaksopplysningerFeil extends DeklarerteFeil {
    SaksopplysningerFeil FACTORY = FeilFactory.create(SaksopplysningerFeil.class);

    @TekniskFeil(feilkode = "FP-258917", feilmelding = "Bruker %s: Finner ikke bruker i TPS", logLevel = LogLevel.WARN)
    Feil feilVedOppslagITPS(String ident);

}
