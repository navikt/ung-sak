package no.nav.k9.sak.mottak.dokumentmottak;

import static no.nav.k9.felles.feil.LogLevel.INFO;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface DokumentmottakMidlertidigFeil extends DeklarerteFeil {

    DokumentmottakMidlertidigFeil FACTORY = FeilFactory.create(DokumentmottakMidlertidigFeil.class);

    @TekniskFeil(feilkode = "K9-653311", feilmelding = "Behandling [%s] pågår, avventer å håndtere mottatt dokument til det er prosessert", logLevel = INFO)
    Feil behandlingPågårAvventerKnytteMottattDokumentTilBehandling(Long behandlingId);

    @TekniskFeil(feilkode = "K9-653312", feilmelding = "Behandling [%s] er i iverksetting-status og kan ikke hoppes tilbake. Avventer å håndtere mottatt dokument til behandlingen er avsluttet", logLevel = INFO)
    Feil behandlingUnderIverksettingAvventerKnytteMottattDokumentTilBehandling(Long behandlingId);
}
