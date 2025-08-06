package no.nav.ung.sak.web.app.tjenester.klage;

import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurdering;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredning;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingEntitet;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.kontrakt.klage.KlageVurderingResultatDto;

import java.util.Optional;

public class KlageVurderingResultatDtoMapper {

    private KlageVurderingResultatDtoMapper() {
    }

    public static Optional<KlageVurderingResultatDto> mapNFPKlageVurderingResultatDto(Behandling behandling, KlageRepository klageRepository, FritekstRepository fritekstRepository) {
        var utredning = klageRepository.hentKlageUtredning(behandling.getId());
        var vurdering = klageRepository.hentVurdering(behandling.getId(), KlageVurdertAv.NAY);
        var fritekst = fritekstRepository.hentFritekst(behandling.getId(), KlageVurdertAv.NAY.getKode()).orElse("");

        return vurdering.map(vurd -> lagDto(utredning, vurd, fritekst));
    }

    public static Optional<KlageVurderingResultatDto> mapNKKlageVurderingResultatDto(Behandling behandling, KlageRepository klageRepository, FritekstRepository fritekstRepository) {
        var utredning = klageRepository.hentKlageUtredning(behandling.getId());
        var vurdering = klageRepository.hentVurdering(behandling.getId(), KlageVurdertAv.NK)
            .or(() -> klageRepository.hentVurdering(behandling.getId(), KlageVurdertAv.NK_KABAL));

        var fritekst = vurdering.map(KlageVurderingEntitet::getVurdertAvEnhet)
            .filter(e -> e == KlageVurdertAv.NK)
            .flatMap(e -> fritekstRepository.hentFritekst(behandling.getId(), KlageVurdertAv.NK.getKode()))
            .orElse("");

        return vurdering.map(vurd -> lagDto(utredning, vurd, fritekst));
    }

    private static KlageVurderingResultatDto lagDto(KlageUtredning utredning, KlageVurderingEntitet klageVurdering, String fritekst) {
        var klageresultat = klageVurdering.getKlageresultat();

        String klageMedholdArsak = klageresultat.getKlageOmgjørÅrsak().equals(KlageMedholdÅrsak.UDEFINERT) ? null : klageresultat.getKlageOmgjørÅrsak().getKode();
        String klageMedholdArsakNavn = klageresultat.getKlageOmgjørÅrsak().equals(KlageMedholdÅrsak.UDEFINERT) ? null : klageresultat.getKlageOmgjørÅrsak().getNavn();
        String klageVurderingOmgjør = klageresultat.getKlageVurderingOmgjør().equals(KlageVurderingOmgjør.UDEFINERT) ? null: klageresultat.getKlageVurderingOmgjør().getKode();
        String klageUtfall = klageVurdering.getKlageVurdering().equals(KlageVurdering.UDEFINERT) ? null : klageVurdering.getKlageVurdering().getKode();
        String hjemmel = klageresultat.getHjemmel() == null ? Hjemmel.MANGLER.getKode() : klageresultat.getHjemmel().getKode();
        KlageVurderingResultatDto dto = new KlageVurderingResultatDto();

        dto.setHjemmel(hjemmel);
        dto.setKlageVurdering(klageUtfall);
        dto.setKlageVurderingOmgjoer(klageVurderingOmgjør);
        dto.setBegrunnelse(klageresultat.getBegrunnelse());
        dto.setFritekstTilBrev(fritekst);
        dto.setKlageMedholdArsak(klageMedholdArsak);
        dto.setKlageMedholdArsakNavn(klageMedholdArsakNavn);
        dto.setKlageVurdertAv(klageVurdering.getVurdertAvEnhet().getKode());
        dto.setGodkjentAvMedunderskriver(utredning.isGodkjentAvMedunderskriver());
        return dto;
    }
}
