package no.nav.ung.sak.klage;

import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;

@Dependent
public class KlageUtredningTjeneste {

    private KlageRepository klageRepository;

    KlageUtredningTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageUtredningTjeneste(KlageRepository klageRepository) {
        this.klageRepository = klageRepository;
    }

    public void oppdaterKlageMedPåklagetEksternBehandlingUuid(Long klageBehandlingId, UUID påklagetBehandlingRef, BehandlingType påklagdBehandlingType) {
        var klageUtredning = klageRepository.hentKlageUtredning(klageBehandlingId);
        klageUtredning.setPåKlagdBehandlingRef(påklagetBehandlingRef);
        klageUtredning.setPåKlagdBehandlingType(påklagdBehandlingType);
//        if (valgtPart != null) {
//            klageUtredning.setKlagendePart(valgtPart);
//        }
        klageRepository.lagre(klageUtredning);
    }
//
//    public void oppdaterKlageMedPart(Long klageBehandlingId, PartEntitet valgtPart) {
//        var klageUtredning = klageRepository.hentKlageUtredning(klageBehandlingId);
//        klageUtredning.setKlagendePart(valgtPart);
//        klageRepository.lagre(klageUtredning);
//    }
}
