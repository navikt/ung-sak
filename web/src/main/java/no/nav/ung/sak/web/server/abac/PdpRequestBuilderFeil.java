package no.nav.ung.sak.web.server.abac;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.ManglerTilgangFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

interface PdpRequestBuilderFeil extends DeklarerteFeil {

    PdpRequestBuilderFeil FACTORY = FeilFactory.create(PdpRequestBuilderFeil.class);

    @TekniskFeil(feilkode = "FP-621834", feilmelding = "Ugyldig input. Støtter bare 0 eller 1 behandling, men har %s", logLevel = LogLevel.WARN)
    Feil ugyldigInputFlereBehandlingIder(Collection<Long> behandlingId);

    @ManglerTilgangFeil(feilkode = "FP-280301", feilmelding = "Ugyldig input. Ikke samsvar mellom behandlingUuid %s og saksnummer %s", logLevel = LogLevel.WARN)
    Feil ugyldigInputManglerSamsvarBehandlingFagsak(UUID behandlingId, String saksnummer);

    @ManglerTilgangFeil(feilkode = "FP-280301", feilmelding = "Ugyldig input. Har behandling %s sammen med mer enn en sak %s", logLevel = LogLevel.WARN)
    Feil ugyldigInputHarFlereSaksnumreMedBehandling(UUID behandlingId, List<String> saksnumre);
}
