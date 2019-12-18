package no.nav.foreldrepenger.økonomi.simulering;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class SimulerOppdragApplikasjonTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SimulerOppdragApplikasjonTjeneste.class);

    @Inject
    public SimulerOppdragApplikasjonTjeneste() {
    }

    /**
     * Generer XMLene som skal sendes over til oppdrag for simulering. Det lages en XML per Oppdrag110.
     * Vi har en Oppdrag110-linje per oppdragsmottaker.
     *
     * @param behandlingId behandling.id
     * @param ventendeTaskId TaskId til ventende prosessTask
     * @return En liste med XMLer som kan sendes over til oppdrag
     */
    public List<String> simulerOppdrag(Long behandlingId, Long ventendeTaskId) {
        log.info("Oppretter simuleringsoppdrag for behandling: {}", behandlingId); //$NON-NLS-1$
        // FIXME K9 Simuler Oppdrag
        return Collections.emptyList();
    }
}
