package no.nav.k9.sak.vilkår;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
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

    public void lagreAvslåttVilkårresultat(BehandlingskontrollKontekst kontekst,
                                           VilkårType vilkårType,
                                           DatoIntervallEntitet vilkårsPeriode,
                                           String begrunnelse,
                                           Avslagsårsak avslagsårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = hentVilkårResultat(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettAvslåttVilkårsResultat(
            behandling,
            vilkårType,
            vilkårene,
            vilkårsPeriode,
            begrunnelse,
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
                                                                String begrunnelse,
                                                                Avslagsårsak avslagsårsak) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(vilkårType)
            .medKantIKantVurderer(getVilkårsPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType()).getKantIKantVurderer());
        vilkårBuilder
            .leggTil(vilkårBuilder
                .hentBuilderFor(vilkårsPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medMerknad(finnVilkårUtfallMerknad(avslagsårsak))
                .medBegrunnelse(begrunnelse)
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
            .medKantIKantVurderer(getVilkårsPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType()).getKantIKantVurderer());
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

    private VilkårsPerioderTilVurderingTjeneste getVilkårsPerioderTilVurderingTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
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
            log.info("Ingen perioder å tilbakestille.");
            return;
        }
        log.info("Setter {} til vurdering", vilkårsPerioder);
        Behandling behandling = hentBehandling(behandlingId);
        var vilkårsPerioderTilVurderingTjeneste = getVilkårsPerioderTilVurderingTjeneste(behandling.getFagsakYtelseType(), behandling.getType());
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

    @WithSpan
    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, @SpanAttribute("vilkarType") VilkårType vilkårType) {
        return utledPerioderTilVurderingUfiltrert(ref, vilkårType);
    }

    public TreeSet<DatoIntervallEntitet> utledPerioderSomIkkeVurderes(BehandlingReferanse ref, VilkårType vilkårType, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var vilkår = hentVilkårResultat(ref.getBehandlingId()).getVilkår(vilkårType);
        var perioder = vilkår.stream().flatMap(v -> v.getPerioder().stream())
            .collect(Collectors.toSet());
        var vilkårsPerioder = perioder.stream().map(VilkårPeriode::getPeriode).collect(Collectors.toSet());
        return vilkårsPerioder.stream()
            .filter(periode -> perioderTilVurdering.stream().noneMatch(p -> p.getFomDato().equals(periode.getFomDato()))).collect(Collectors.toCollection(TreeSet::new));
    }

    public TreeSet<DatoIntervallEntitet> utledPerioderTilVurderingUfiltrert(BehandlingReferanse ref, VilkårType vilkårType) {
        var perioderTilVurderingTjeneste = getVilkårsPerioderTilVurderingTjeneste(ref.getFagsakYtelseType(), ref.getBehandlingType());
        var perioder = new TreeSet<>(perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), vilkårType));
        var utvidetTilVUrdering = perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(ref);
        if (!utvidetTilVUrdering.isEmpty()) {
            perioder.addAll(utvidetTilVUrdering);
        }
        return perioder;
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
    public LocalDateTimeline<VilkårUtfallSamlet> samletVilkårsresultat(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (vilkårene.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        LocalDateTimeline<Boolean> allePerioder = vilkårene.get().getAlleIntervaller();
        if (allePerioder.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        var maksPeriode = DatoIntervallEntitet.fra(allePerioder.getMinLocalDate(), allePerioder.getMaxLocalDate());
        return samleVilkårUtfall(vilkårene.get(), maksPeriode);
    }

    LocalDateTimeline<VilkårUtfallSamlet> samleVilkårUtfall(Vilkårene vilkårene, DatoIntervallEntitet maksPeriode) {
        var timelinePerVilkår = vilkårene.getVilkårTidslinjer(maksPeriode);
        Set<VilkårType> alleForventedeVilkårTyper = timelinePerVilkår.keySet();
        var timeline = new LocalDateTimeline<List<VilkårUtfall>>(List.of());

        for (var e : timelinePerVilkår.entrySet()) {
            LocalDateTimeline<VilkårUtfall> utfallTimeline = e.getValue().mapValue(v -> new VilkårUtfall(e.getKey(), v.getAvslagsårsak(), v.getUtfall()));
            timeline = timeline.crossJoin(utfallTimeline.compress(), StandardCombinators::allValues);
        }

        var samletUtfall = timeline.mapValue(VilkårUtfallSamlet::fra)
            .filterValue(v -> v.getUnderliggendeVilkårUtfall().stream().map(VilkårUtfall::getVilkårType).collect(Collectors.toSet()).containsAll(alleForventedeVilkårTyper));
        log.info("forventendeVilkårTyper={}, maksPeriode={}, timelinePerVilkår={}, samletUtfall={}", alleForventedeVilkårTyper, maksPeriode, timelinePerVilkår, samletUtfall);

        return samletUtfall;
    }


    LocalDateTimeline<VilkårUtfallSamlet> samletVilkårUtfall(Map<VilkårType, LocalDateTimeline<VilkårPeriode>> timelinePerVilkår, Set<VilkårType> minimumVilkår) {
        var timeline = new LocalDateTimeline<List<VilkårUtfall>>(List.of());

        for (var e : timelinePerVilkår.entrySet()) {
            LocalDateTimeline<VilkårUtfall> utfallTimeline = e.getValue().mapValue(v -> new VilkårUtfall(e.getKey(), v.getAvslagsårsak(), v.getUtfall()));
            timeline = timeline.crossJoin(utfallTimeline.compress(), StandardCombinators::allValues);
        }

        var resultat = timeline.mapValue(VilkårUtfallSamlet::fra)
            .filterValue(v -> v.getUnderliggendeVilkårUtfall().stream().map(VilkårUtfall::getVilkårType).collect(Collectors.toSet()).containsAll(minimumVilkår));
        return resultat;
    }

}
