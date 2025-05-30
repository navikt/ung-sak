//TODO bør ligge i en annen pakke da det ikke er revurdering spesifikt?
package no.nav.ung.sak.behandling.revurdering.etterkontroll.tjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@Dependent
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

    public void utfør(Behandling behandling, String etterkontrollId) {
        var etterkontroll = etterkontrollRepository.hent(etterkontrollId);
        if (etterkontroll.isBehandlet()) {
            log.info("Etterkontroll av kontrolltype = {} var allerede behandlet med id = {}", etterkontroll.getKontrollType(), etterkontrollId);
            return;
        }

        log.info("Utfører etterkontroll av type = {} og kontrollTidspunkt = {} ",
                etterkontroll.getKontrollType(), etterkontroll.getKontrollTidspunkt());

        var kontrollTjeneste = KontrollTjeneste.finnTjeneste(kontrollTjenester, behandling.getFagsakYtelseType(), etterkontroll.getKontrollType());

        var utført = kontrollTjeneste.utfør(etterkontroll);
        if (utført) {
            log.info("Utført etterkontroll av type = {}", etterkontroll.getKontrollType());
            etterkontroll.setErBehandlet(true);
            etterkontrollRepository.lagre(etterkontroll);
        } else {
            log.info("Etterkontroll ble ikke utført");
        }
    }
}
