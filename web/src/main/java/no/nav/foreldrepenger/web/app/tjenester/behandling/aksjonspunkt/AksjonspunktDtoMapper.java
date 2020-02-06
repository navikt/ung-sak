package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktDto;

class AksjonspunktDtoMapper {

    private AksjonspunktDtoMapper() {
    }

    static Set<AksjonspunktDto> lagAksjonspunktDto(Behandling behandling, Collection<Totrinnsvurdering> ttVurderinger) {
        return behandling.getAksjonspunkter().stream()
                .filter(aksjonspunkt -> !aksjonspunkt.erAvbrutt())
                .map(aksjonspunkt -> mapFra(aksjonspunkt, behandling, ttVurderinger))
                .collect(Collectors.toSet());
    }

    private static AksjonspunktDto mapFra(Aksjonspunkt aksjonspunkt, Behandling behandling, Collection<Totrinnsvurdering> ttVurderinger) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();

        AksjonspunktDto dto = new AksjonspunktDto();
        dto.setDefinisjon(aksjonspunktDefinisjon);
        dto.setStatus(aksjonspunkt.getStatus());
        dto.setBegrunnelse(aksjonspunkt.getBegrunnelse());
        dto.setVilkarType(finnVilkårType(aksjonspunkt));
        dto.setToTrinnsBehandling(aksjonspunkt.isToTrinnsBehandling() || aksjonspunktDefinisjon.getDefaultTotrinnBehandling());
        dto.setFristTid(aksjonspunkt.getFristTid());

        Optional<Totrinnsvurdering> vurdering = ttVurderinger.stream().filter(v -> v.getAksjonspunktDefinisjon() == aksjonspunkt.getAksjonspunktDefinisjon()).findFirst();
        vurdering.ifPresent(ttVurdering -> {
            dto.setBesluttersBegrunnelse(ttVurdering.getBegrunnelse());
            dto.setToTrinnsBehandlingGodkjent(ttVurdering.isGodkjent());
            dto.setVurderPaNyttArsaker(ttVurdering.getVurderPåNyttÅrsaker().stream()
                .map(VurderÅrsakTotrinnsvurdering::getÅrsaksType).collect(Collectors.toSet()));
            }
        );

        dto.setAksjonspunktType(aksjonspunktDefinisjon.getAksjonspunktType());
        dto.setKanLoses(kanLøses(aksjonspunktDefinisjon, behandling));
        dto.setErAktivt(Boolean.TRUE);
        return dto;
    }

    private static VilkårType finnVilkårType(Aksjonspunkt aksjonspunkt) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();
        return aksjonspunktDefinisjon.getVilkårType();
    }

    private static Boolean kanLøses(AksjonspunktDefinisjon def, Behandling behandling) {
        if (behandling.getBehandlingStegStatus() == null) {
            // Stegstatus ikke satt, kan derfor ikke sette noen aksjonspunkt som løsbart
            return false;
        }
        if (def.erAutopunkt()) {
            return false;
        }
        Optional<BehandlingStegType> aktivtBehandlingSteg = Optional.ofNullable(behandling.getAktivtBehandlingSteg());
        return aktivtBehandlingSteg.map(steg ->
                skalLøsesIStegKode(def, behandling.getBehandlingStegStatus().getKode(), steg))
                .orElse(false);
    }

    private static Boolean skalLøsesIStegKode(AksjonspunktDefinisjon def, String stegKode, BehandlingStegType steg) {
        if (BehandlingStegStatus.INNGANG.getKode().equals(stegKode)) {
            return steg.getAksjonspunktDefinisjonerInngang().contains(def);
        } else
            return BehandlingStegStatus.UTGANG.getKode().equals(stegKode) && steg.getAksjonspunktDefinisjonerUtgang().contains(def);
    }
}
