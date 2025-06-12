package no.nav.ung.sak.vilkår;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.*;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet.VilkårUtfall;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

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


    /**
     * Gjenoppretter vilkårsutfall for perioder som ikke lenger vurderes i behandlingen
     *
     * @param referanse            Behandlingreferanse
     * @param vilkårType           Vilkårtype som skal gjenopprettes
     * @param perioderTilVurdering Perioder som vurderes i behandlingen
     * @param tillatFjerning       Tillater fjerning av vilkårsperioder
     * @return Resultat for hvilke perioder som ble kopiert og hvilke som eventuelt ble fjernet
     */
    public GjenopprettPerioderResultat gjenopprettVilkårsutfallForPerioderSomIkkeVurderes(BehandlingReferanse referanse, VilkårType vilkårType, Collection<DatoIntervallEntitet> perioderTilVurdering, boolean tillatFjerning) {
        if (referanse.getOriginalBehandlingId().isEmpty()) {
            return new GjenopprettPerioderResultat(Collections.emptyList(), Collections.emptyList());
        }
        var gjenopprettetPeriodeListe = finnPerioderForGjenopprettingAvVilkårsutfall(referanse, vilkårType, perioderTilVurdering);
        if (!gjenopprettetPeriodeListe.isEmpty()) {
            log.info("Gjenoppretter initiell vurdering for perioder {}", gjenopprettetPeriodeListe);
            return kopierOriginaltVilkårresultat(
                referanse.getBehandlingId(), referanse.getOriginalBehandlingId().get(),
                gjenopprettetPeriodeListe, vilkårType, tillatFjerning);
        }
        return new GjenopprettPerioderResultat(Collections.emptyList(), Collections.emptyList());
    }


    /**
     * Kopierer vilkårsresultat fra forrige behandling for gitt vilkår og perioder
     *
     * @param behandlingId         BehandlingId
     * @param originalBehandlingId BehandlingId for original behandling
     * @param perioder             Perioder som skal kopieres
     * @param vilkårType           Vilkårtype for periode som skal kopieres
     * @param tillatFjerning       Tillater fjerning av vilkårsperioder
     * @return Resultat for hvilke perioder som ble kopiert og hvilke som eventuelt ble fjernet
     */
    public GjenopprettPerioderResultat kopierOriginaltVilkårresultat(Long behandlingId,
                                                                     Long originalBehandlingId,
                                                                     Set<DatoIntervallEntitet> perioder,
                                                                     VilkårType vilkårType, boolean tillatFjerning) {
        var vilkårResultat = hentVilkårResultat(behandlingId);
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultat);
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(vilkårType);
        var resultat = kopierOriginaltVilkårresultat(originalBehandlingId, perioder, vilkårType, vilkårBuilder, tillatFjerning);
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
        return resultat;
    }

    private GjenopprettPerioderResultat kopierOriginaltVilkårresultat(Long originalBehandlingId, Set<DatoIntervallEntitet> perioder, VilkårType vilkårType, VilkårBuilder vilkårBuilder, boolean tillatFjerning) {
        var vedtattUtfallPåVilkåret = hentHvisEksisterer(originalBehandlingId)
            .orElseThrow()
            .getVilkår(vilkårType)
            .orElseThrow();

        var gjenopprettetPerioder = new ArrayList<DatoIntervallEntitet>();
        var fjernetPerioder = new ArrayList<DatoIntervallEntitet>();
        for (var periode : perioder) {
            var eksisteredeVurdering = finnVilkårsresultatForKopi(periode, vedtattUtfallPåVilkåret, vilkårType);
            if (eksisteredeVurdering.isPresent()) {
                var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(periode).forlengelseAv(eksisteredeVurdering.get());
                vilkårBuilder.leggTil(vilkårPeriodeBuilder);
                gjenopprettetPerioder.add(periode);
            } else if (tillatFjerning) {
                vilkårBuilder.tilbakestill(periode);
                fjernetPerioder.add(periode);
                log.info("Fjerner periode {}", periode);
            } else {
                throw new IllegalStateException("Fant ikke periode " + periode + " i forrige behandling for vilkår " + vilkårType);
            }
        }
        return new GjenopprettPerioderResultat(gjenopprettetPerioder, fjernetPerioder);
    }

    private static Optional<VilkårPeriode> finnVilkårsresultatForKopi(DatoIntervallEntitet periode, Vilkår vedtattUtfallPåVilkåret, VilkårType vilkårType) {
        var vilkårPeriodeForSkjæringstidspunkt = vedtattUtfallPåVilkåret.finnPeriodeForSkjæringstidspunktHvisFinnes(periode.getFomDato());
        if (vilkårPeriodeForSkjæringstidspunkt.isEmpty() && erLøpendeVilkår(vilkårType)) {
            // Tidligere slo vi sammen medlemskapsperioder for ulike vilkårsperioder til en sammenhengende periode
            // Dersom vi ikkje finner ein eksakt match på stp ser vi etter periode som overlapper for vilkår som vurderes løpende
            // Vilkår som vurderes løpende vurderes ikkje kun ved stp, men kan få endret status avhengig av endringer i registerdata
            return vedtattUtfallPåVilkåret.finnPeriodeSomInneholderDato(periode.getFomDato());
        }
        return vilkårPeriodeForSkjæringstidspunkt;
    }

    private static boolean erLøpendeVilkår(VilkårType vilkårType) {
        return Set.of(VilkårType.UNGDOMSPROGRAMVILKÅRET, VilkårType.ALDERSVILKÅR).contains(vilkårType);
    }

    private Set<DatoIntervallEntitet> finnPerioderForGjenopprettingAvVilkårsutfall(BehandlingReferanse ref, VilkårType vilkårType, Collection<DatoIntervallEntitet> perioderTilVurdering) {
        var vilkårOptional = hentHvisEksisterer(ref.getBehandlingId()).flatMap(v -> v.getVilkår(vilkårType));
        if (vilkårOptional.isPresent()) {
            return finnPerioderSomIkkeVurderes(ref, vilkårType, perioderTilVurdering);
        }
        return Collections.emptySet();
    }

    private Set<DatoIntervallEntitet> finnPerioderSomIkkeVurderes(BehandlingReferanse ref,
                                                                  VilkårType vilkårType, Collection<DatoIntervallEntitet> perioderTilVurdering) {
        return utledPerioderSomIkkeVurderes(ref, vilkårType, perioderTilVurdering);
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

    public TreeSet<DatoIntervallEntitet> utledPerioderSomIkkeVurderes(BehandlingReferanse ref, VilkårType vilkårType, Collection<DatoIntervallEntitet> perioderTilVurdering) {
        var vilkår = hentVilkårResultat(ref.getBehandlingId()).getVilkår(vilkårType);
        var perioder = vilkår.stream().flatMap(v -> v.getPerioder().stream())
            .collect(Collectors.toSet());
        var vilkårsPerioder = perioder.stream().map(VilkårPeriode::getPeriode).collect(Collectors.toSet());
        return vilkårsPerioder.stream()
            .filter(vilkårsperiode -> perioderTilVurdering.stream()
                .noneMatch(tilVurdering -> tilVurdering.inkluderer(vilkårsperiode.getFomDato()))).collect(Collectors.toCollection(TreeSet::new));
    }

    public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId);
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

    public record GjenopprettPerioderResultat(Collection<DatoIntervallEntitet> gjenopprettetPerioder,
                                              Collection<DatoIntervallEntitet> fjernetPerioder) {
    }

}
