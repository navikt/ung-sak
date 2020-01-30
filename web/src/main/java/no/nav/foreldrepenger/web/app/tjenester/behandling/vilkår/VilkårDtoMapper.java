package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;


class VilkårDtoMapper {

    private VilkårDtoMapper() {
        // SONAR - Utility classes should not have public constructors
    }

    static List<VilkårDto> lagVilkarDto(Behandling behandling, boolean medVilkårkjøring, Vilkårene vilkårene) {
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        if (behandlingsresultat != null) {
            if (vilkårene != null) {
                return vilkårene.getVilkårene().stream().flatMap(vilkår -> {
                    return vilkår.getPerioder().stream().map(it -> {
                        VilkårDto dto = new VilkårDto();
                        dto.setAvslagKode(it.getAvslagsårsak() != null ? it.getAvslagsårsak().getKode() : null);
                        dto.setVilkarType(vilkår.getVilkårType());
                        dto.setLovReferanse(vilkår.getVilkårType().getLovReferanse(behandling.getFagsakYtelseType()));
                        dto.setVilkarStatus(it.getGjeldendeUtfall());
                        dto.setMerknadParametere(it.getMerknadParametere());
                        dto.setOverstyrbar(erOverstyrbar(vilkår, behandling));

                        if (medVilkårkjøring) {
                            dto.setInput(it.getRegelInput());
                            dto.setEvaluering(it.getRegelEvaluering());
                        }

                        return dto;
                    });
                }).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    // Angir om vilkåret kan overstyres (forutsetter at bruker har tilgang til å overstyre)
    private static boolean erOverstyrbar(@SuppressWarnings("unused") Vilkår vilkår, Behandling behandling) {
        if (behandling.erÅpnetForEndring()) {
            return true;
        }
        return !behandling.erAvsluttet();
    }
}
