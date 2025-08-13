package no.nav.ung.sak.domene.vedtak.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingEntitet;

@ApplicationScoped
public class KlageVedtakTjeneste {

    KlageRepository klageRepository;

    @Inject
    public KlageVedtakTjeneste(KlageRepository klageRepository) {
        this.klageRepository = klageRepository;
    }

    public KlageVedtakTjeneste() {}


    public boolean erKlageResultatHjemsendt(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentKlageUtredning(behandling.getId())
                .erKlageHjemsendt();
        }
        return false;
    }

    private boolean erKlageGodkjentHosMedunderskriver(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            var vurdertAvKlageInstans = klageRepository.hentGjeldendeVurdering(behandling)
                .map(KlageVurderingEntitet::getVurdertAvEnhet)
                .map(vurdertAv -> vurdertAv.equals(KlageVurdertAv.NK))
                .orElse(Boolean.FALSE);

            if (vurdertAvKlageInstans) {
                return klageRepository.hentKlageUtredning(behandling.getId())
                    .isGodkjentAvMedunderskriver();
            } else {
                return false;
            }
        }
        return false;
    }

}
