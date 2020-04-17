package no.nav.k9.sak.web.app.tjenester.fagsak;

import java.time.LocalDateTime;

import no.nav.k9.sak.web.app.tjenester.VurderProsessTaskStatusForPollingApi.ProsessTaskFeilmelder;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

interface FagsakProsessTaskFeil extends DeklarerteFeil, ProsessTaskFeilmelder {

    String FEIL_I_TASK = "FP-293308";
    String FORSINKELSE_I_TASK = "FP-293309";
    String FORSINKELSE_VENTER_SVAR = "FP-293310";

    @Override
    @TekniskFeil(feilkode = FEIL_I_TASK, feilmelding = "[%1$s]. Forespørsel på fagsak [id=%2$s] som ikke kan fortsette, Problemer med task gruppe [%3$s]. Siste prosesstask[id=%5$s, type=%4$s] status=%6$s, sistKjørt=%7$s", logLevel = LogLevel.WARN)
    Feil feilIProsessTaskGruppe(String callId, Long fagsakId, String gruppe, String taskType, Long taskId, ProsessTaskStatus taskStatus, LocalDateTime sistKjørt);

    @Override
    @TekniskFeil(feilkode = FORSINKELSE_I_TASK, feilmelding = "[%1$s]. Forespørsel på fagsak [id=%2$s] som er utsatt i påvente av task [id=%5$s, type=%4$s], Gruppe [%3$s] kjøres ikke før senere. Task status=%6$s, planlagt neste kjøring=%7$s", logLevel = LogLevel.WARN)
    Feil utsattKjøringAvProsessTask(String callId, Long fagsakId, String gruppe, String taskType, Long taskId, ProsessTaskStatus taskStatus, LocalDateTime nesteKjøringEtter);


    @Override
    @TekniskFeil(feilkode = FORSINKELSE_VENTER_SVAR, feilmelding = "[%1$s]. Forespørsel på behandling [id=%2$s] som venter på svar fra annet system (task [id=%5$s, type=%4$s], gruppe [%3$s] kjøres ikke før det er mottatt). Task status=%6$s, sistKjørt=%7$s", logLevel = LogLevel.WARN)
    Feil venterPåSvar(String callId, Long entityId, String gruppe, String taskType, Long id, ProsessTaskStatus status, LocalDateTime sistKjørt);

}
