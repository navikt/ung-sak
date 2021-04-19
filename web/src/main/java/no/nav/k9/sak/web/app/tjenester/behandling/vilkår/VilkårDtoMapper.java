package no.nav.k9.sak.web.app.tjenester.behandling.vilkår;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.vilkår.VilkårMedPerioderDto;
import no.nav.k9.sak.kontrakt.vilkår.VilkårPeriodeDto;
import no.nav.k9.sak.typer.Periode;

class VilkårDtoMapper {

    private static final Logger logger = LoggerFactory.getLogger(VilkårDtoMapper.class);

    private VilkårDtoMapper() {
        // SONAR - Utility classes should not have public constructors
    }

    static List<VilkårMedPerioderDto> lagVilkarMedPeriodeDto(Behandling behandling, boolean medVilkårkjøring, Vilkårene vilkårene, Map<VilkårType, Set<DatoIntervallEntitet>> aktuelleVilkårsperioder) {
        if (vilkårene != null) {
            return vilkårene.getVilkårene()
                .stream()
                .filter(it -> !VilkårType.OPPTJENINGSPERIODEVILKÅR.equals(it.getVilkårType()))
                .map(vilkår -> mapVilkår(vilkår, medVilkårkjøring, behandling, aktuelleVilkårsperioder))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static VilkårPeriodeDto mapTilPeriode(boolean medVilkårkjøring, VilkårPeriode it, Set<DatoIntervallEntitet> aktuelleVilkårsperioder, VilkårType vilkårType) {
        VilkårPeriodeDto dto = new VilkårPeriodeDto();
        dto.setPeriode(new Periode(it.getFom(), it.getTom()));
        dto.setAvslagKode(it.getAvslagsårsak() != null ? it.getAvslagsårsak().getKode() : null);
        dto.setVilkarStatus(it.getGjeldendeUtfall());
        dto.setMerknadParametere(it.getMerknadParametere());
        dto.setBegrunnelse(it.getBegrunnelse());
        dto.setVurderesIBehandlingen(vurdersIBehandlingen(it, aktuelleVilkårsperioder, vilkårType));

        if (medVilkårkjøring) {
            dto.setInput(it.getRegelInput());
            dto.setEvaluering(it.getRegelEvaluering());
        }
        return dto;
    }

    private static boolean vurdersIBehandlingen(VilkårPeriode vilkårPeriode, Set<DatoIntervallEntitet> aktuelleVilkårsperioder, VilkårType vilkårType) {
        if (aktuelleVilkårsperioder == null) {
            return false;
        }
        boolean match = aktuelleVilkårsperioder.contains(vilkårPeriode.getPeriode());
        if (match) {
            return true;
        }
        for (DatoIntervallEntitet aktuellVilkårsperiode : aktuelleVilkårsperioder) {
            if (aktuellVilkårsperiode.overlapper(vilkårPeriode.getPeriode())) {
                String feilmelding = "Delvis treff på vilkårsperiodesammenligning (forventet kun hele treff). VilkårTyp=" + vilkårType + " VilkårPeriode " + vilkårPeriode.getPeriode() + " overlapper delvis periode fra " + aktuelleVilkårsperioder;
                if (Environment.current().isProd()) {
                    //unngår hard validering i prod før funksjonalitet har vært testet og verifisert
                    logger.warn(feilmelding);
                } else {
                    throw new IllegalStateException(feilmelding);
                }
            }
        }
        return false;
    }

    private static VilkårMedPerioderDto mapVilkår(Vilkår vilkår, boolean medVilkårkjøring, Behandling behandling, Map<VilkårType, Set<DatoIntervallEntitet>> aktuelleVilkårsperioder) {
        var vilkårsPerioder = vilkår.getPerioder().stream().map(it -> mapTilPeriode(medVilkårkjøring, it, aktuelleVilkårsperioder.get(vilkår.getVilkårType()), vilkår.getVilkårType())).collect(Collectors.toList());
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
