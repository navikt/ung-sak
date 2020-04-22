package no.nav.k9.sak.behandlingslager.task;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskEvent;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;

/**
 * Fjerner nøkler for logging fra task tråd når task er kjørt (eller feilet).
 * Disse nøklene initieres typisk i BehandlingskontrollTjenesteImpl#init... eller i Abac request.
 */
@ApplicationScoped
public class ProsessTaskLogContext {
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$

    public void observeProsessTask(@Observes ProsessTaskEvent ptEvent) {
        if (ptEvent.getNyStatus() != null && !ptEvent.getNyStatus().equals(ProsessTaskStatus.KLAR)) {
            LOG_CONTEXT.clear();
        }
    }
}
