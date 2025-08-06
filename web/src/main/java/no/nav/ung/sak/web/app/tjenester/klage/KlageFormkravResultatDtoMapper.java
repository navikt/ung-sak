package no.nav.ung.sak.web.app.tjenester.klage;

import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravAdapter;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredning;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingEntitet;
import no.nav.ung.sak.kontrakt.klage.KlageFormkravResultatDto;

import java.util.Optional;

class KlageFormkravResultatDtoMapper {
    private KlageFormkravResultatDtoMapper() {
    }

    static Optional<KlageFormkravResultatDto> mapNFPKlageFormkravResultatDto(Behandling behandling, KlageRepository klageRepository) {
        var klageFormkrav = klageRepository.hentVurdering(behandling.getId(), KlageVurdertAv.NAY)
            .map(KlageVurderingEntitet::getFormkrav);
        KlageUtredning klageUtredning = klageRepository.hentKlageUtredning(behandling.getId());

        return klageFormkrav.map((KlageFormkravAdapter formkrav) -> lagDto(formkrav, klageUtredning));
    }

    static Optional<KlageFormkravResultatDto> mapKAKlageFormkravResultatDto(Behandling behandling, KlageRepository klageRepository) {
        var klageFormkrav = klageRepository.hentVurdering(behandling.getId(), KlageVurdertAv.NK)
            .map(KlageVurderingEntitet::getFormkrav);
        KlageUtredning klageUtredning = klageRepository.hentKlageUtredning(behandling.getId());

        return klageFormkrav.map((KlageFormkravAdapter formkrav) -> lagDto(formkrav, klageUtredning));
    }

    private static KlageFormkravResultatDto lagDto(KlageFormkravAdapter klageFormkrav, KlageUtredning klageUtredning) {
        KlageFormkravResultatDto dto = new KlageFormkravResultatDto();
        dto.setPaKlagdBehandlingRef(klageUtredning.getPåKlagBehandlingRef().orElse(null));
        dto.setPåklagdBehandlingType(klageUtredning.getPåKlagdBehandlingType().orElse(null));
        dto.setBegrunnelse(klageFormkrav.getBegrunnelse());
        dto.setErKlagerPart(klageFormkrav.isErKlagerPart());
        dto.setErKlageKonkret(klageFormkrav.isErKonkret());
        dto.setErKlagefirstOverholdt(klageFormkrav.isErFristOverholdt());
        dto.setErSignert(klageFormkrav.isErSignert());
        dto.setAvvistArsaker(klageFormkrav.hentAvvistÅrsaker());
        return dto;
    }

}
