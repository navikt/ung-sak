package no.nav.ung.sak.inngangsvilkår;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public class RegelResultat {
    private final Vilkårene vilkårene;
    private final Set<AksjonspunktResultat> aksjonspunktDefinisjoner;

    private final Map<VilkårType, Map<DatoIntervallEntitet, Object>> ekstraResultater;

    public RegelResultat(Vilkårene vilkårene,
                         List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner,
                         Map<VilkårType, Map<DatoIntervallEntitet, Object>> ekstraResultater) {
        this.vilkårene = vilkårene;
        this.aksjonspunktDefinisjoner = aksjonspunktDefinisjoner.stream()
            .map(AksjonspunktResultat::opprettForAksjonspunkt)
            .collect(Collectors.toCollection(HashSet::new));
        this.ekstraResultater = ekstraResultater;
    }

    public Vilkårene getVilkårene() {
        return vilkårene;
    }

    public Map<VilkårType, Object> getEkstraResultater() {
        return Collections.unmodifiableMap(ekstraResultater);
    }

    public Map<VilkårType, Map<DatoIntervallEntitet, Object>> getEkstraResultaterPerPeriode() {
        return Collections.unmodifiableMap(ekstraResultater);
    }

    public Set<AksjonspunktResultat> getAksjonspunktDefinisjoner() {
        return aksjonspunktDefinisjoner;
    }

    public <V> Optional<V> getEkstraResultat(VilkårType vilkårType) {
        @SuppressWarnings("unchecked")
        V val = (V) ekstraResultater.get(vilkårType);
        return Optional.ofNullable(val);
    }

    public boolean vilkårErVurdert(LocalDate fom, LocalDate tom, VilkårType vilkårType) {
        final var berørtePerioder = vilkårene
            .getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(vilkårType))
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
            .collect(Collectors.toList());
        return berørtePerioder.stream().noneMatch(it -> it.getUtfall().equals(Utfall.IKKE_VURDERT));
    }

    public boolean vilkårErIkkeOppfylt(LocalDate fom, LocalDate tom, VilkårType vilkårType) {
        final var berørtePerioder = vilkårene
            .getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(vilkårType))
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
            .collect(Collectors.toList());
        return berørtePerioder.stream().noneMatch(it -> it.getUtfall().equals(Utfall.OPPFYLT));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "vilkårene=" + vilkårene
            + ", aksjonspunkt=" + aksjonspunktDefinisjoner
            + ", ekstraResultater=" + ekstraResultater
            + ">";
    }
}
