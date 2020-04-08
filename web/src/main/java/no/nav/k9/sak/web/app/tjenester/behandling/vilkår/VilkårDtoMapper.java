package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.kontrakt.vilkår.VilkårDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårPeriodeDto;
import no.nav.k9.sak.typer.Periode;

class VilkårDtoMapper {

    private VilkårDtoMapper() {
        // SONAR - Utility classes should not have public constructors
    }

    static List<VilkårDto> lagVilkarDto(Behandling behandling, boolean medVilkårkjøring, Vilkårene vilkårene) {
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
        return Collections.emptyList();
    }

    static List<VilkårMedPerioderDto> lagVilkarMedPeriodeDto(Behandling behandling, boolean medVilkårkjøring, Vilkårene vilkårene) {
        if (vilkårene != null) {
            return vilkårene.getVilkårene().stream().map(vilkår -> mapVilkår(vilkår, medVilkårkjøring, behandling)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static VilkårPeriodeDto mapTilPeriode(boolean medVilkårkjøring, VilkårPeriode it) {
        VilkårPeriodeDto dto = new VilkårPeriodeDto();
        dto.setPeriode(new Periode(it.getFom(), it.getTom()));
        dto.setAvslagKode(it.getAvslagsårsak() != null ? it.getAvslagsårsak().getKode() : null);
        dto.setVilkarStatus(it.getGjeldendeUtfall());
        dto.setMerknadParametere(it.getMerknadParametere());

        if (medVilkårkjøring) {
            dto.setInput(it.getRegelInput());
            dto.setEvaluering(it.getRegelEvaluering());
        }

        return dto;
    }

    private static VilkårMedPerioderDto mapVilkår(Vilkår vilkår, boolean medVilkårkjøring, Behandling behandling) {
        var vilkårsPerioder = vilkår.getPerioder().stream().map(it -> mapTilPeriode(medVilkårkjøring, it)).collect(Collectors.toList());
        var vilkårMedPerioderDto = new VilkårMedPerioderDto(vilkår.getVilkårType(), vilkårsPerioder);
        vilkårMedPerioderDto.setLovReferanse(vilkår.getVilkårType().getLovReferanse(behandling.getFagsakYtelseType()));
        vilkårMedPerioderDto.setOverstyrbar(erOverstyrbar(vilkår, behandling));
        return vilkårMedPerioderDto;
    }

    // Angir om vilkåret kan overstyres (forutsetter at bruker har tilgang til å overstyre)
    private static boolean erOverstyrbar(@SuppressWarnings("unused") Vilkår vilkår, Behandling behandling) {
        if (behandling.erÅpnetForEndring()) {
            return true;
        }
        return !behandling.erAvsluttet();
    }
}
