package no.nav.k9.sak.behandling.prosessering;

import static no.nav.vedtak.feil.LogLevel.INFO;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public interface ProsesseringsFeil extends DeklarerteFeil {

    ProsesseringsFeil FACTORY = FeilFactory.create(ProsesseringsFeil.class);

    @TekniskFeil(feilkode = "K9-655544", feilmelding = "Kan ikke kjøre fordi annen task blokkerer: %s", logLevel = INFO)
    Feil kanIkkePlanleggeNyTaskPgaAlleredePlanlagtetask(ProsessTaskData data);

    @TekniskFeil(feilkode = "K9-858930", feilmelding = "Kan ikke kjøre fordi behandlingen er vurdert komplett men ikke klargjort med registerinnhenting: %s", logLevel = INFO)
    Feil kanIkkePlanleggeNyTaskPgaVentendeTaskerPåBehandling(Long behandlingId);
}
