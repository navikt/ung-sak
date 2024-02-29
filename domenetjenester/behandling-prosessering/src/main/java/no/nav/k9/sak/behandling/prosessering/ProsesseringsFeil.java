package no.nav.k9.sak.behandling.prosessering;

import static no.nav.k9.felles.feil.LogLevel.INFO;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;

public interface ProsesseringsFeil extends DeklarerteFeil {

    ProsesseringsFeil FACTORY = FeilFactory.create(ProsesseringsFeil.class);

    @TekniskFeil(feilkode = "K9-655544", feilmelding = "Kan ikke kjøre fordi annen task blokkerer [id=%s, type=%s, status=%s]", logLevel = INFO)
    Feil kanIkkePlanleggeNyTaskPgaAlleredePlanlagtetask(Long id, String taskType, ProsessTaskStatus status);

    @TekniskFeil(feilkode = "K9-858930", feilmelding = "Kan ikke kjøre fordi behandlingen er vurdert komplett men ikke klargjort med registerinnhenting: %s", logLevel = INFO)
    Feil kanIkkePlanleggeNyTaskPgaVentendeTaskerPåBehandling(Long behandlingId);
}
