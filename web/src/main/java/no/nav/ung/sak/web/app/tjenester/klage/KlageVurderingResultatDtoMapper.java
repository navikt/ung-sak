package no.nav.ung.sak.web.app.tjenester.klage;

import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingEntitet;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.kontrakt.klage.KlageVurderingResultatDto;

import java.util.Optional;

public class KlageVurderingResultatDtoMapper {

    private KlageVurderingResultatDtoMapper() {
    }

    public static Optional<KlageVurderingResultatDto> mapFørsteinstansKlageVurderingResultatDto(Behandling behandling, KlageRepository klageRepository, FritekstRepository fritekstRepository) {
        var utredning = klageRepository.hentKlageUtredning(behandling.getId());
        var vurdering = utredning.hentKlagevurdering(KlageVurdertAv.VEDTAKSINSTANS);
        var fritekst = fritekstRepository.hentFritekst(behandling.getId(), KlageVurdertAv.VEDTAKSINSTANS.getKode()).orElse("");

        return vurdering.map(vurd -> lagDto(utredning, vurd, fritekst));
    }

    public static Optional<KlageVurderingResultatDto> mapAndreinstansKlageVurderingResultatDto(Behandling behandling, KlageRepository klageRepository, FritekstRepository fritekstRepository) {
        var utredning = klageRepository.hentKlageUtredning(behandling.getId());
        var vurdering = utredning.hentKlagevurdering(KlageVurdertAv.KLAGEINSTANS);

        var fritekst = vurdering.map(KlageVurderingEntitet::getVurdertAvEnhet)
            .filter(e -> e == KlageVurdertAv.KLAGEINSTANS)
            .flatMap(e -> fritekstRepository.hentFritekst(behandling.getId(), KlageVurdertAv.KLAGEINSTANS.getKode()))
            .orElse("");

        return vurdering.map(vurd -> lagDto(utredning, vurd, fritekst));
    }

    private static KlageVurderingResultatDto lagDto(KlageUtredningEntitet utredning, KlageVurderingEntitet klageVurdering, String fritekst) {
        var klageresultat = klageVurdering.getKlageresultat();

        String klageMedholdArsak = klageresultat.getKlageOmgjørÅrsak().map(KlageMedholdÅrsak::getKode).orElse(null);
        String klageMedholdArsakNavn = klageresultat.getKlageOmgjørÅrsak().map(KlageMedholdÅrsak::getNavn).orElse(null);
        String klageVurderingOmgjør = klageresultat.getKlageVurderingOmgjør().map(KlageVurderingOmgjør::getKode).orElse(null);
        String klageUtfall = klageresultat.getKlageVurdering().map(KlageVurderingType::getKode).orElse(null);
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
