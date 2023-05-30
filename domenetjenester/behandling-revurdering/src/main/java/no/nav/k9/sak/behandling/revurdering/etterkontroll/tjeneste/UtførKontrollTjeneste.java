//TODO bør ligge i en annen pakke da det ikke er revurdering spesifikt?
package no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
public class UtførKontrollTjeneste {

    private static final Logger log = LoggerFactory.getLogger(UtførKontrollTjeneste.class);

    private EtterkontrollRepository etterkontrollRepository;
    private Instance<KontrollTjeneste> kontrollTjenester;

    UtførKontrollTjeneste() {
        // CDI pga ApplicationScoped
    }

    @Inject
    public UtførKontrollTjeneste(EtterkontrollRepository etterkontrollRepository, @Any Instance<KontrollTjeneste> kontrollTjenester) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.kontrollTjenester = kontrollTjenester;
    }

    public void utfør(Behandling behandling, boolean kunAktuellBehandling) {
        var etterkontroller = etterkontrollRepository.finnEtterkontrollForFagsak(behandling.getFagsakId())
            .stream()
            .filter(it -> !it.isBehandlet())
            .filter(it -> !kunAktuellBehandling || (it.getBehandlingId() != null && Objects.equals(it.getBehandlingId(), behandling.getId())))
            .toList();

        for (Etterkontroll etterkontroll : etterkontroller) {
            log.info("Utfører etterkontroll av type = {}, for behandling = {}", etterkontroll.getKontrollType(), behandling.getId());
            var kontrollTjeneste = KontrollTjeneste.finnTjeneste(kontrollTjenester, behandling.getFagsakYtelseType(), etterkontroll.getKontrollType());

            var utført = kontrollTjeneste.utfør(etterkontroll);
            if (utført) {
                log.info("Utført etterkontroll av type = {}, for behandling = {}", etterkontroll.getKontrollType(), behandling.getId());
                etterkontroll.setErBehandlet(true);
                etterkontrollRepository.lagre(etterkontroll);
            }
        }
    }
}
