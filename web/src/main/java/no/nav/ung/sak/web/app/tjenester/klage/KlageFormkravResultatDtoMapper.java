package no.nav.ung.sak.web.app.tjenester.klage;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravAdapter;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.kontrakt.klage.KlageFormkravResultatDto;

import java.util.Optional;

class KlageFormkravResultatDtoMapper {
    private KlageFormkravResultatDtoMapper() {
    }

    static Optional<KlageFormkravResultatDto> mapNFPKlageFormkravResultatDto(Behandling behandling, KlageRepository klageRepository) {
        KlageUtredningEntitet klageUtredning = klageRepository.hentKlageUtredning(behandling.getId());
        var klageFormkrav = klageUtredning.getFormkrav().map(KlageFormkravEntitet::tilFormkrav);

        return klageFormkrav.map((KlageFormkravAdapter formkrav) -> lagDto(formkrav, klageUtredning));
    }

    private static KlageFormkravResultatDto lagDto(KlageFormkravAdapter klageFormkrav, KlageUtredningEntitet klageUtredning) {
        KlageFormkravResultatDto dto = new KlageFormkravResultatDto();
        dto.setPaKlagdBehandlingRef(klageUtredning.getpåklagdBehandlingRef().orElse(null));
        dto.setPåklagdBehandlingType(klageUtredning.getpåklagdBehandlingType().orElse(null));
        dto.setBegrunnelse(klageFormkrav.getBegrunnelse());
        dto.setErKlagerPart(klageFormkrav.isErKlagerPart());
        dto.setErKlageKonkret(klageFormkrav.isErKonkret());
        dto.setErKlagefirstOverholdt(klageFormkrav.isFristOverholdt());
        dto.setErSignert(klageFormkrav.isErSignert());
        dto.setAvvistArsaker(klageFormkrav.hentAvvistÅrsaker());
        return dto;
    }

}
