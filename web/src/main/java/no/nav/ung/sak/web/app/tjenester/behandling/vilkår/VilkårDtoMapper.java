package no.nav.ung.sak.web.app.tjenester.behandling.vilkår;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.ung.sak.kontrakt.vilkår.VilkårPeriodeDto;
import no.nav.ung.sak.typer.Periode;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class VilkårDtoMapper {

    private VilkårDtoMapper() {
        // SONAR - Utility classes should not have public constructors
    }

    static List<VilkårMedPerioderDto> lagVilkarMedPeriodeDto(Behandling behandling, boolean medVilkårkjøring, Vilkårene vilkårene) {
        if (vilkårene != null) {
            return vilkårene.getVilkårene()
                .stream()
                .map(vilkår -> mapVilkår(vilkår, medVilkårkjøring, behandling))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static VilkårPeriodeDto mapTilPeriode(boolean medVilkårkjøring, VilkårPeriode it) {
        VilkårPeriodeDto dto = new VilkårPeriodeDto();
        dto.setPeriode(new Periode(it.getFom(), it.getTom()));
        dto.setAvslagKode(it.getAvslagsårsak() != null ? it.getAvslagsårsak().getKode() : null);
        dto.setVilkarStatus(it.getGjeldendeUtfall());
        dto.setMerknadParametere(it.getMerknadParametere());
        dto.setBegrunnelse(it.getBegrunnelse());
        dto.setMerknad(it.getMerknad());
        dto.setVurderesIBehandlingen(true);

        if (medVilkårkjøring) {
            dto.setInput(it.getRegelInput());
            dto.setEvaluering(it.getRegelEvaluering());
        }
        return dto;
    }

    private static VilkårMedPerioderDto mapVilkår(Vilkår vilkår, boolean medVilkårkjøring, Behandling behandling) {
        var vilkårsPerioder = vilkår.getPerioder().stream().map(it -> mapTilPeriode(medVilkårkjøring, it))
            .collect(Collectors.toList());
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
