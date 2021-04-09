package no.nav.k9.sak.vilkår;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet.VilkårUtfall;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class VilkårTjeneste {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(VilkårTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    protected VilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public VilkårTjeneste(BehandlingRepository behandlingRepository,
                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                          VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    public Vilkårene hentVilkårResultat(Long behandlingId) {
        return vilkårResultatRepository.hent(behandlingId);
    }

    public List<VilkårPeriodeResultatDto> hentVilkårResultater(Long behandlingId) {
        return vilkårResultatRepository.hentVilkårResultater(behandlingId);
    }

    public void lagreAvslåttVilkårresultat(BehandlingskontrollKontekst kontekst,
                                           VilkårType vilkårType,
                                           DatoIntervallEntitet vilkårsPeriode,
                                           Avslagsårsak avslagsårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = hentVilkårResultat(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettAvslåttVilkårsResultat(
            behandling,
            vilkårType,
            vilkårene,
            vilkårsPeriode,
            avslagsårsak);
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    public void lagreVilkårresultat(BehandlingskontrollKontekst kontekst,
                                    VilkårType vilkårType,
                                    DatoIntervallEntitet vilkårsPeriode,
                                    Avslagsårsak avslagsårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = hentVilkårResultat(kontekst.getBehandlingId());
        boolean vilkårOppfylt = avslagsårsak == null;
        VilkårResultatBuilder vilkårResultatBuilder = opprettVilkårsResultat(vilkårene, vilkårType, vilkårsPeriode, behandling, avslagsårsak);
        if (!vilkårOppfylt) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        }
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private VilkårResultatBuilder opprettAvslåttVilkårsResultat(Behandling behandling,
                                                                VilkårType vilkårType,
                                                                Vilkårene vilkårene,
                                                                DatoIntervallEntitet vilkårsPeriode,
                                                                Avslagsårsak avslagsårsak) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(vilkårType)
            .medKantIKantVurderer(getVilkårsPerioderTilVurderingTjeneste(behandling).getKantIKantVurderer());
        vilkårBuilder
            .leggTil(vilkårBuilder
                .hentBuilderFor(vilkårsPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medMerknad(finnVilkårUtfallMerknad(avslagsårsak))
                .medAvslagsårsak(avslagsårsak));
        builder.leggTil(vilkårBuilder);
        return builder;
    }

    private VilkårUtfallMerknad finnVilkårUtfallMerknad(Avslagsårsak avslagsårsak) {
        return VilkårUtfallMerknad.fraKode(avslagsårsak.getKode());
    }

    private VilkårResultatBuilder opprettVilkårsResultat(Vilkårene vilkårene, VilkårType vilkårType, DatoIntervallEntitet vilkårsPeriode, Behandling behandling, Avslagsårsak avslagsårsak) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(vilkårType)
            .medKantIKantVurderer(getVilkårsPerioderTilVurderingTjeneste(behandling).getKantIKantVurderer());
        boolean oppfylt = avslagsårsak == null;
        vilkårBuilder
            .leggTil(vilkårBuilder
                .hentBuilderFor(vilkårsPeriode)
                .medUtfall(oppfylt ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT)
                .medMerknad(oppfylt ? VilkårUtfallMerknad.UDEFINERT : VilkårUtfallMerknad.VM_1041)
                .medAvslagsårsak(oppfylt ? null : avslagsårsak));
        builder.leggTil(vilkårBuilder);
        return builder;
    }

    private VilkårsPerioderTilVurderingTjeneste getVilkårsPerioderTilVurderingTjeneste(Behandling behandling) {
        var ytelseType = behandling.getFagsakYtelseType();
        var behandlingType = behandling.getType();
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, ytelseType, behandlingType)
            .orElseThrow(() -> new UnsupportedOperationException(
                "VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + ytelseType + "], behandlingtype [" + behandlingType + "]"));
    }

    public void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst, VilkårType vilkårType, DatoIntervallEntitet vilkårsPeriode) {
        settVilkårutfallTilIkkeVurdert(kontekst.getBehandlingId(), vilkårType, new TreeSet<>(List.of(vilkårsPeriode)));
        nullstillBehandlingsresultat(kontekst);
    }

    public void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst, VilkårType vilkårType, NavigableSet<DatoIntervallEntitet> vilkårsPerioder) {
        settVilkårutfallTilIkkeVurdert(kontekst.getBehandlingId(), vilkårType, vilkårsPerioder);
        nullstillBehandlingsresultat(kontekst);
    }

    public void settVilkårutfallTilIkkeVurdert(Long behandlingId, VilkårType vilkårType, NavigableSet<DatoIntervallEntitet> vilkårsPerioder) {
        if (vilkårsPerioder.isEmpty()) {
            return;
        }
        Behandling behandling = hentBehandling(behandlingId);
        var vilkårsPerioderTilVurderingTjeneste = getVilkårsPerioderTilVurderingTjeneste(behandling);
        vilkårResultatRepository.tilbakestillPerioder(behandlingId, vilkårType, vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer(), vilkårsPerioder);
    }

    private void nullstillBehandlingsresultat(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = hentBehandling(behandlingId);
        if (Objects.equals(behandling.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }
        behandling.setBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Behandling hentBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, VilkårType vilkårType, boolean skalIgnorereAvslåttePerioder) {
        Long behandlingId = ref.getBehandlingId();
        var behandling = hentBehandling(behandlingId);
        var perioderTilVurderingTjeneste = getVilkårsPerioderTilVurderingTjeneste(behandling);

        var vilkår = hentHvisEksisterer(behandlingId).flatMap(it -> it.getVilkår(vilkårType));
        var perioder = new TreeSet<>(perioderTilVurderingTjeneste.utled(behandlingId, vilkårType));
        var utvidetTilVUrdering = perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(ref);

        if (!utvidetTilVUrdering.isEmpty()) {
            log.info("Fikk utvidet perioder til vurdering {}, i tillegg til vilkårsperioder: {}", utvidetTilVUrdering, perioder);
            perioder.addAll(utvidetTilVUrdering);
        }

        if (vilkår.isPresent() && skalIgnorereAvslåttePerioder) {
            var avslåttePerioder = vilkår.get()
                .getPerioder()
                .stream()
                .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))
                .map(VilkårPeriode::getPeriode)
                .collect(Collectors.toList());
            perioder.removeAll(avslåttePerioder);
        }
        return Collections.unmodifiableNavigableSet(perioder);
    }

    public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId);
    }

    public boolean erNoenVilkårHeltAvslått(Long behandlingId, VilkårType vilkårType, LocalDate fom, LocalDate tom) {
        var vilkårene = hentVilkårResultat(behandlingId);
        if (vilkårene.getHarAvslåtteVilkårsPerioder()) {
            boolean heltAvslått = true;
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            for (var v : vilkårene.getVilkårTidslinjer(periode).entrySet()) {
                var timeline = v.getValue();
                if (timeline.isEmpty()) {
                    continue;
                }
                if (v.getKey() == vilkårType) {
                    // skip oss selv
                    continue;
                }
                var altAvslått = timeline.toSegments().stream().allMatch(vp -> vp.getValue().getUtfall() == Utfall.IKKE_OPPFYLT);
                if (altAvslått) {
                    log.info("Alle perioder avslått for vilkår {}, maksPeriode={}", v.getKey(), periode);
                    return true;
                }

                heltAvslått = false;
            }
            return heltAvslått;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public LocalDateTimeline<VilkårUtfallSamlet> samletVilkårsresultat(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (vilkårene.isEmpty()) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var tjeneste = getVilkårsPerioderTilVurderingTjeneste(behandling);
        List<DatoIntervallEntitet> allePerioder = tjeneste.utled(behandlingId).values().stream().flatMap(v -> v.stream()).sorted().collect(Collectors.toList());

        if (allePerioder.isEmpty()) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        }
        var maksPeriode = DatoIntervallEntitet.minmax(allePerioder);
        var timelinePerVilkår = vilkårene.get().getVilkårTidslinjer(maksPeriode);

        Set<VilkårType> alleForventedeVilkårTyper = timelinePerVilkår.keySet();
        return samletVilkårUtfall(timelinePerVilkår, alleForventedeVilkårTyper);
    }

    LocalDateTimeline<VilkårUtfallSamlet> samletVilkårUtfall(Map<VilkårType, LocalDateTimeline<VilkårPeriode>> timelinePerVilkår, Set<VilkårType> minimumVilkår) {
        var timeline = new LocalDateTimeline<List<VilkårUtfall>>(List.of());

        for (var e : timelinePerVilkår.entrySet()) {
            LocalDateTimeline<VilkårUtfall> utfallTimeline = e.getValue().mapValue(v -> new VilkårUtfall(e.getKey(), v.getAvslagsårsak(), v.getUtfall()));
            timeline = timeline.crossJoin(utfallTimeline, StandardCombinators::allValues);
        }

        var resultat = timeline.mapValue(v -> VilkårUtfallSamlet.fra(v))
            .filterValue(v -> v.getUnderliggendeVilkårUtfall().stream().map(VilkårUtfall::getVilkårType).collect(Collectors.toSet()).containsAll(minimumVilkår));
        return resultat;
    }
}
