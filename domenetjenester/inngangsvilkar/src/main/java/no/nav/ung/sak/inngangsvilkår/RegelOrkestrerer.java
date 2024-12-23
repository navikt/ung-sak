package no.nav.ung.sak.inngangsvilkår;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class RegelOrkestrerer {

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    protected RegelOrkestrerer() {
        // For CDI
    }

    @Inject
    public RegelOrkestrerer(InngangsvilkårTjeneste inngangsvilkårTjeneste, VilkårResultatRepository vilkårResultatRepository) {
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> perioder) {
        Objects.requireNonNull(perioder, "Perioden som skal vurderes må være satt");
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        List<Vilkår> matchendeVilkårPåBehandling = vilkårene.getVilkårene()
            .stream()
            .filter(v -> vilkårHåndtertAvSteg.contains(v.getVilkårType()))
            .collect(toList());
        validerMaksEttVilkår(matchendeVilkårPåBehandling);

        Vilkår vilkår = matchendeVilkårPåBehandling.isEmpty() ? null : matchendeVilkårPåBehandling.get(0);
        if (vilkår == null || perioder.isEmpty()) {
            // Intet vilkår skal eksekveres i regelmotor
            return new RegelResultat(vilkårene, emptyList(), emptyMap());
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        Map<VilkårType, Map<DatoIntervallEntitet, Object>> ekstraResultater = new HashMap<>();

        var regelResultat = kjørRegelmotor(ref, vilkår, perioder);
        for (var entry : regelResultat.entrySet()) {
            // Ekstraresultat
            var vilkårDataResultat = entry.getValue();
            if (vilkårDataResultat.getEkstraVilkårresultat() != null) {
                final var ekstradataMap = ekstraResultater.getOrDefault(vilkårDataResultat.getVilkårType(), new HashMap<>());
                ekstradataMap.put(vilkårDataResultat.getPeriode(), vilkårDataResultat.getEkstraVilkårresultat());
                ekstraResultater.put(vilkårDataResultat.getVilkårType(), ekstradataMap);
            }

            // Inngangsvilkårutfall utledet fra alle vilkårsutfallene
            vilkårene = oppdaterBehandlingMedVilkårresultat(vilkårDataResultat, vilkårene);
            // Aksjonspunkter
            aksjonspunktDefinisjoner.addAll(vilkårDataResultat.getApDefinisjoner());
        }

        return new RegelResultat(vilkårene, aksjonspunktDefinisjoner, ekstraResultater);
    }

    private void validerMaksEttVilkår(List<Vilkår> vilkårSomSkalBehandle) {
        if (vilkårSomSkalBehandle.size() > 1)
            throw new IllegalArgumentException("Kun ett vilkår skal evalueres per regelkall. " +
                "Her angis vilkår: " + vilkårSomSkalBehandle.stream().map(v -> v.getVilkårType().getKode()).collect(Collectors.joining(",")));
    }

    protected NavigableMap<DatoIntervallEntitet, VilkårData> vurderVilkår(VilkårType vilkårType, BehandlingReferanse ref, NavigableSet<DatoIntervallEntitet> periode) {
        Inngangsvilkår inngangsvilkår = inngangsvilkårTjeneste.finnVilkår(vilkårType, ref.getFagsakYtelseType());
        return inngangsvilkår.vurderVilkår(ref, periode);
    }

    private NavigableMap<DatoIntervallEntitet, VilkårData> kjørRegelmotor(BehandlingReferanse ref, Vilkår vilkår, NavigableSet<DatoIntervallEntitet> perioder) {
        if (perioder.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        return vurderVilkår(vilkår.getVilkårType(), ref, perioder);
    }

    private Vilkårene oppdaterBehandlingMedVilkårresultat(VilkårData vilkårData, Vilkårene vilkårene) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);

        final var vilkårBuilder = builder.hentBuilderFor(vilkårData.getVilkårType());
        final var periode = vilkårData.getPeriode();
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
            .medUtfall(vilkårData.getUtfallType())
            .medMerknadParametere(vilkårData.getMerknadParametere())
            .medRegelEvaluering(vilkårData.getRegelEvaluering())
            .medRegelInput(vilkårData.getRegelInput())
            .medAvslagsårsak(vilkårData.getAvslagsårsak())
            .medMerknad(vilkårData.getVilkårUtfallMerknad()));
        builder.leggTil(vilkårBuilder);

        return builder.build();
    }
}
