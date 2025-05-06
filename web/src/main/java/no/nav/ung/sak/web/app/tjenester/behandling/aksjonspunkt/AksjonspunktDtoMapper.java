package no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.kontrakt.aksjonspunkt.AksjonspunktDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.ung.sak.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;

import java.util.*;
import java.util.stream.Collectors;

class AksjonspunktDtoMapper {

    private AksjonspunktDtoMapper() {
    }

    static Set<AksjonspunktDto> lagAksjonspunktDto(Behandling behandling, Collection<Totrinnsvurdering> ttVurderinger) {
        return behandling.getAksjonspunkter().stream()
            .filter(aksjonspunkt -> !aksjonspunkt.erAvbrutt())
            .map(aksjonspunkt -> mapFra(aksjonspunkt, behandling, ttVurderinger))
            .collect(Collectors.toSet());
    }

    static AksjonspunktDto mapFra(Aksjonspunkt aksjonspunkt, Behandling behandling) {
        return mapFra(aksjonspunkt, behandling, null);
    }

    static List<AksjonspunktDto> mapFra(Behandling behandling, List<Aksjonspunkt> aksjonspunkter) {
        if (aksjonspunkter == null || aksjonspunkter.isEmpty()) {
            return Collections.emptyList();
        }
        List<AksjonspunktDto> result = new ArrayList<>();
        for (var a : aksjonspunkter) {
            result.add(mapFra(a, behandling));
        }
        return result;
    }

    static AksjonspunktDto mapFra(Aksjonspunkt aksjonspunkt, Behandling behandling, Collection<Totrinnsvurdering> ttVurderinger) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();

        AksjonspunktDto dto = new AksjonspunktDto();
        dto.setDefinisjon(aksjonspunktDefinisjon);
        dto.setStatus(aksjonspunkt.getStatus());
        dto.setBegrunnelse(aksjonspunkt.getBegrunnelse());
        dto.setVilkarType(finnVilkårType(aksjonspunkt));
        dto.setToTrinnsBehandling(aksjonspunkt.isToTrinnsBehandling() || aksjonspunktDefinisjon.getDefaultTotrinnBehandling());
        dto.setFristTid(aksjonspunkt.getFristTid());
        dto.setVenteårsak(aksjonspunkt.getVenteårsak());
        dto.setVenteårsakVariant(aksjonspunkt.getVenteårsakVariant());
        dto.setOpprettetAv(aksjonspunkt.getOpprettetAv());

        if (ttVurderinger != null && !ttVurderinger.isEmpty()) {
            Optional<Totrinnsvurdering> vurdering = ttVurderinger.stream().filter(v -> v.getAksjonspunktDefinisjon() == aksjonspunkt.getAksjonspunktDefinisjon()).findFirst();
            vurdering.ifPresent(ttVurdering -> {
                dto.setBesluttersBegrunnelse(ttVurdering.getBegrunnelse());
                dto.setToTrinnsBehandlingGodkjent(ttVurdering.isGodkjent());
                dto.setVurderPaNyttArsaker(ttVurdering.getVurderPåNyttÅrsaker().stream()
                    .map(VurderÅrsakTotrinnsvurdering::getÅrsaksType).collect(Collectors.toSet()));
            });
        }

        dto.setAksjonspunktType(aksjonspunktDefinisjon.getAksjonspunktType());
        dto.setKanLoses(kanLøses(aksjonspunktDefinisjon, behandling));
        dto.setErAktivt(Boolean.TRUE);
        return dto;
    }

    private static VilkårType finnVilkårType(Aksjonspunkt aksjonspunkt) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();
        return aksjonspunktDefinisjon.getVilkårType();
    }

    public static Boolean kanLøses(AksjonspunktDefinisjon def, Behandling behandling) {
        if (behandling.getBehandlingStegStatus() == null) {
            // Stegstatus ikke satt, kan derfor ikke sette noen aksjonspunkt som løsbart
            return false;
        }
        if (def.erAutopunkt()) {
            return false;
        }
        Optional<BehandlingStegType> aktivtBehandlingSteg = Optional.ofNullable(behandling.getAktivtBehandlingSteg());
        return aktivtBehandlingSteg.map(steg -> skalLøsesIStegKode(def, behandling.getBehandlingStegStatus().getKode(), steg))
            .orElse(false);
    }

    private static Boolean skalLøsesIStegKode(AksjonspunktDefinisjon def, String stegKode, BehandlingStegType steg) {
        return BehandlingStegStatus.UTGANG.getKode().equals(stegKode) && steg.getAksjonspunktDefinisjonerUtgang().contains(def);
    }
}
