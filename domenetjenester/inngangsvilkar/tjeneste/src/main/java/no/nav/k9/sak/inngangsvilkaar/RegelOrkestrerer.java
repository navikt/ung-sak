package no.nav.k9.sak.inngangsvilkaar;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static no.nav.k9.sak.inngangsvilkaar.RegelintegrasjonFeil.FEILFACTORY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

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

    public RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, BehandlingReferanse ref, List<DatoIntervallEntitet> perioder) {
        Objects.requireNonNull(perioder, "Perioden som skal vurderes må være satt");
        var vilkårene = vilkårResultatRepository.hent(ref.getBehandlingId());
        List<Vilkår> matchendeVilkårPåBehandling = vilkårene.getVilkårene().stream()
            .filter(v -> vilkårHåndtertAvSteg.contains(v.getVilkårType()))
            .collect(toList());
        validerMaksEttVilkår(matchendeVilkårPåBehandling);

        Vilkår vilkår = matchendeVilkårPåBehandling.isEmpty() ? null : matchendeVilkårPåBehandling.get(0);
        if (vilkår == null) {
            // Intet vilkår skal eksekveres i regelmotor
            return new RegelResultat(vilkårene, emptyList(), emptyMap());
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        Map<VilkårType, Map<DatoIntervallEntitet, Object>> ekstraResultater = new HashMap<>();

        for (DatoIntervallEntitet periode : perioder) {
            VilkårData vilkårDataResultat = kjørRegelmotor(ref, vilkår, periode);
            // Ekstraresultat
            ekstraResultater = new HashMap<>();
            if (vilkårDataResultat.getEkstraVilkårresultat() != null) {
                final var ekstradataMap = ekstraResultater.getOrDefault(vilkårDataResultat.getVilkårType(), new HashMap<>());
                ekstradataMap.put(vilkårDataResultat.getPeriode(), vilkårDataResultat.getEkstraVilkårresultat());
                ekstraResultater.put(vilkårDataResultat.getVilkårType(), ekstradataMap);
            }

            // Inngangsvilkårutfall utledet fra alle vilkårsutfallene
            vilkårene = oppdaterBehandlingMedVilkårresultat(vilkårDataResultat, vilkårene);
            // Aksjonspunkter
            if (!erPeriodenOverstyrt(vilkår, periode)) {
                aksjonspunktDefinisjoner.addAll(vilkårDataResultat.getApDefinisjoner());
            }
        }

        return new RegelResultat(vilkårene, aksjonspunktDefinisjoner, ekstraResultater);
    }

    private boolean erPeriodenOverstyrt(Vilkår vilkår, DatoIntervallEntitet periode) {
        return vilkår.getPerioder()
            .stream()
            .filter(it -> it.getPeriode().equals(periode))
            .map(VilkårPeriode::getErOverstyrt)
            .findFirst()
            .orElse(false);
    }

    private void validerMaksEttVilkår(List<Vilkår> vilkårSomSkalBehandle) {
        if (vilkårSomSkalBehandle.size() > 1)
            throw new IllegalArgumentException("Kun ett vilkår skal evalueres per regelkall. " +
                "Her angis vilkår: " + vilkårSomSkalBehandle.stream().map(v -> v.getVilkårType().getKode()).collect(Collectors.joining(",")));
    }

    protected VilkårData vurderVilkår(VilkårType vilkårType, BehandlingReferanse ref, DatoIntervallEntitet periode) {
        Inngangsvilkår inngangsvilkår = inngangsvilkårTjeneste.finnVilkår(vilkårType, ref.getFagsakYtelseType());
        return inngangsvilkår.vurderVilkår(ref, periode);
    }

    private VilkårData kjørRegelmotor(BehandlingReferanse ref, Vilkår vilkår, DatoIntervallEntitet periode) {
        return vurderVilkår(vilkår.getVilkårType(), ref, periode);
    }

    public VilkårResultatType utledInngangsvilkårUtfall(Collection<Utfall> vilkårene) {
        boolean oppfylt = vilkårene.stream()
            .anyMatch(utfall -> utfall.equals(Utfall.OPPFYLT));
        boolean ikkeOppfylt = vilkårene.stream()
            .anyMatch(vilkår -> vilkår.equals(Utfall.IKKE_OPPFYLT));
        boolean ikkeVurdert = vilkårene.stream()
            .anyMatch(vilkår -> vilkår.equals(Utfall.IKKE_VURDERT));

        // Enkeltutfallene per vilkår sammenstilles til et samlet vilkårsresultat.
        // Høyest rangerte enkeltutfall ift samlet vilkårsresultat sjekkes først, deretter nest høyeste osv.
        VilkårResultatType resultatType;
        if (ikkeOppfylt) {
            resultatType = VilkårResultatType.AVSLÅTT;
        } else if (ikkeVurdert) {
            resultatType = VilkårResultatType.IKKE_FASTSATT;
        } else if (oppfylt) {
            resultatType = VilkårResultatType.INNVILGET;
        } else {
            throw FEILFACTORY.kanIkkeUtledeVilkårsresultatFraRegelmotor().toException();
        }

        return resultatType;
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
