package no.nav.k9.sak.behandlingslager.fagsak;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

interface FagsakFeil extends DeklarerteFeil {

    FagsakFeil FACTORY = FeilFactory.create(FagsakFeil.class);

    @TekniskFeil(feilkode = "FP-429883", feilmelding = "Det var flere enn en Fagsak for saksnummer: %s", logLevel = LogLevel.WARN)
    Feil flereEnnEnFagsakForSaksnummer(Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-081717", feilmelding = "Bruker har skiftet rolle fra '%s' til '%s'", logLevel = LogLevel.WARN)
    Feil brukerHarSkiftetRolle(String gammelKode, String nyKode);

    @TekniskFeil(feilkode = "FP-831923", feilmelding = "Prøver å koble fagsak med saksnummer %s sammen med seg selv", logLevel = LogLevel.WARN)
    Feil kanIkkeKobleMedSegSelv(Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-983410", feilmelding = "Kan ikke koble sammen saker med forskjellig ytelse type. Prøver å koble sammen fagsakene %s (%s) og %s (%s).", logLevel = LogLevel.WARN)
    Feil kanIkkeKobleSammenSakerMedUlikYtelseType(Long fagsakEn, FagsakYtelseType sakEnType, Long fagsakTo, FagsakYtelseType sakToType);

    @TekniskFeil(feilkode = "FP-102432", feilmelding = "Kan ikke koble sammen to saker med identisk aktørid. Prøver å koble sammen fagsakene %s og %s, aktør %s.", logLevel = LogLevel.WARN)
    Feil kanIkkeKobleSammenToSakerMedSammeAktørId(Saksnummer saksnummer, Saksnummer saksnummer1, AktørId aktørId);

    @TekniskFeil(feilkode = "FP-102433", feilmelding = "Fagsakene %s og %s er ikke koblet.", logLevel = LogLevel.WARN)
    Feil sakeneErIkkeKoblet(Saksnummer saksnummer, Saksnummer saksnummer1);

    @TekniskFeil(feilkode = "FP-102434", feilmelding = "Fagsakene %s og %s kan ikke frakobles.", logLevel = LogLevel.WARN)
    Feil kanIkkeFraKobleSakene(Saksnummer saksnummer, Saksnummer saksnummer1);

}
