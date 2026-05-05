package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.bosatt.BosattSøknadGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaringHolder;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BOSTED;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BOSTED)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderBosattSteg extends VilkårVurderingSteg {

    private static final Duration DEFAULT_VENTEFRIST = Duration.ofDays(14);

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private BosattSøknadGrunnlagRepository bosattSøknadGrunnlagRepository;

    VurderBosattSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderBosattSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                            VilkårResultatRepository vilkårResultatRepository,
                            VilkårTjeneste vilkårTjeneste,
                            BehandlingRepository behandlingRepository,
                            BostedsGrunnlagRepository bostedsGrunnlagRepository,
                            BosattSøknadGrunnlagRepository bosattSøknadGrunnlagRepository,
                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                            EtterlysningTjeneste etterlysningTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.bosattSøknadGrunnlagRepository = bosattSøknadGrunnlagRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.BOSTEDSVILKÅR;
    }

    @Override
    public Set<VilkårType> getVilkårAvhengigheter(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        EnumSet<VilkårType> avhengigheter = EnumSet.noneOf(VilkårType.class);
        avhengigheter.add(VilkårType.ALDERSVILKÅR);
        avhengigheter.add(VilkårType.SØKNADSFRIST);
        avhengigheter.addAll(manuelleVilkårRekkefølgeTjeneste.finnManuelleVilkårSomErFør(getAktuellVilkårType(), ytelseType, behandlingType));
        return avhengigheter;
    }

    @Override
    public BehandleStegResultat utførResten(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = finnPerioderSomSkalVurderes(kontekst);

        if (tidslinjeTilVurdering.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<EtterlysningData> etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(
            behandlingId, kontekst.getFagsakId(), EtterlysningType.UTTALELSE_BOSTED);
        Map<LocalDate, EtterlysningData> etterlysningPerFom = etterlysninger.stream()
            .collect(Collectors.toMap(e -> e.periode().getFomDato(), e -> e));

        Set<LocalDate> ventendeFom = new LinkedHashSet<>();
        Set<DatoIntervallEntitet> ferdigePerioder = new LinkedHashSet<>();
        Set<LocalDate> trengerManuellVurderingFom = new LinkedHashSet<>();
        Set<LocalDate> trengerSaksbehandlerFom = new LinkedHashSet<>();

        initierFaktaFraSøknadsdata(behandlingId, tidslinjeTilVurdering);

        tidslinjeTilVurdering.stream().forEach(segment -> {
            LocalDate fom = segment.getFom();
            EtterlysningData etterlysning = etterlysningPerFom.get(fom);

            if (etterlysning == null) {
                // Grunnlag finnes, ingen etterlysning → ferdig (auto-vurdering)
                ferdigePerioder.add(DatoIntervallEntitet.fraOgMedTilOgMed(fom, segment.getTom()));
            } else if (erVentende(etterlysning)) {
                ventendeFom.add(fom);
            } else if (erFerdigUtenUttalelse(etterlysning)) {
                ferdigePerioder.add(DatoIntervallEntitet.fraOgMedTilOgMed(fom, segment.getTom()));
            } else if (harMottattSvarMedUttalelse(etterlysning)) {
                trengerManuellVurderingFom.add(fom);
            }
        });

        // Saksbehandler må vurdere bosted for perioder uten grunnlag — prioritert over vent
        if (!trengerSaksbehandlerFom.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BOSTED));
        }
        // Etterlysning sendt, svar ikke mottatt ennå — sett behandling på vent
        if (!ventendeFom.isEmpty()) {
            return settPåVent(ventendeFom, etterlysningPerFom);
        }
        // Auto-vurder alle ferdigperioder (ingen etterlysning, utløpt, eller svar uten uttalelse)
        autoVurder(behandlingId, ferdigePerioder);
        // Finn perioder som krever manuell vurdering av vilkåret
        LocalDateTimeline<Boolean> trengerManuell = finnPerioderSomTrengerManuellVurdering(behandlingId, tidslinjeTilVurdering, trengerManuellVurderingFom);
        if (!trengerManuell.isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_BOSTEDSVILKÅR));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    // Dette kan vurderes å flyttes til persistering av søknad. Ulempen er at vi må då må ta stilling til vilkårsperioder tidlig
    private void initierFaktaFraSøknadsdata(long behandlingId, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        Map<LocalDate, Boolean> søknadErBosattPerFom = bosattSøknadGrunnlagRepository.hentSøknadBostedPerFom(behandlingId);
        // Auto-sett fakta fra søknad for perioder uten grunnlag
        Map<LocalDate, Boolean> nyeSøknadAvklaringer = new LinkedHashMap<>();
        tidslinjeTilVurdering.stream().forEach(segment -> {
            LocalDate fom = segment.getFom();
            Boolean søknadVerdi = finnSøknadverdi(søknadErBosattPerFom, fom);
            if (søknadVerdi != null) {
                nyeSøknadAvklaringer.put(fom, søknadVerdi);
            }
        });
        if (!nyeSøknadAvklaringer.isEmpty()) {
            bostedsGrunnlagRepository.lagreAvklaringerFraSøknad(behandlingId, nyeSøknadAvklaringer);
        }
    }

    private static Boolean finnSøknadverdi(Map<LocalDate, Boolean> søknadErBosattPerFom, LocalDate fom) {
        // Kan vi forvente eksakte datoer her?
        return søknadErBosattPerFom.get(fom);
    }

    private static BehandleStegResultat settPåVent(Set<LocalDate> ventendeFom, Map<LocalDate, EtterlysningData> etterlysningPerFom) {
        LocalDateTime frist = ventendeFom.stream()
            .map(fom -> Optional.ofNullable(etterlysningPerFom.get(fom)).map(EtterlysningData::frist).orElse(null))
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(LocalDateTime.now().plus(DEFAULT_VENTEFRIST));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
            AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon(),
                EtterlysningType.UTTALELSE_BOSTED.mapTilVenteårsak(),
                frist
            )
        ));
    }

    /**
     * Finn perioder som krever manuell vilkårsvurdering:
     * - Mottatt uttalelse fra bruker (harMottattUttalelseFom)
     * - Fastsatt årsak=ANNET
     * - Kilde=SØKNAD
     */
    private LocalDateTimeline<Boolean> finnPerioderSomTrengerManuellVurdering(long behandlingId, LocalDateTimeline<Boolean> tidslinjeTilVurdering, Set<LocalDate> harMottattUttalelseFom) {
        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Forventer grunnlag med bostedsavklaringer"));
        BostedsAvklaringHolder holder = grunnlag.getHolder();
        List<LocalDateSegment<Boolean>> segmenterMedManuellVilkårsvurdering = tidslinjeTilVurdering.stream().filter(segment -> {
            BostedsPeriodeAvklaring avklaring = holder.getPeriodeAvklaring(segment.getFom()).orElseThrow(() -> new IllegalStateException("Forventer å finne en bostedsperiodeavklaring for "));
            return skalVurderesManuelt(harMottattUttalelseFom, segment, avklaring);
        }).toList();
        return new LocalDateTimeline<>(segmenterMedManuellVilkårsvurdering);
    }

    private static boolean skalVurderesManuelt(Set<LocalDate> harMottattUttalelseFom, LocalDateSegment<Boolean> segment, BostedsPeriodeAvklaring avklaring) {
        boolean erÅrsakSomSkalVurderesManuelt = FraflyttingsÅrsak.ANNET.equals(avklaring.getFraflyttingsÅrsak());
        boolean erKildeSøknad = Kilde.SØKNAD.equals(avklaring.getKilde());
        boolean harUttalelseFraBruker = harMottattUttalelseFom.contains(segment.getFom());
        return erÅrsakSomSkalVurderesManuelt || erKildeSøknad || harUttalelseFraBruker;
    }

    private static boolean erVentende(EtterlysningData etterlysning) {
        return etterlysning.status() == EtterlysningStatus.OPPRETTET
            || etterlysning.status() == EtterlysningStatus.VENTER;
    }

    private static boolean erFerdigUtenUttalelse(EtterlysningData etterlysning) {
        return etterlysning.status() == EtterlysningStatus.UTLØPT
            || (etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
            && etterlysning.uttalelseData() != null
            && !etterlysning.uttalelseData().harUttalelse());
    }

    private static boolean harMottattSvarMedUttalelse(EtterlysningData etterlysning) {
        return etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
            && etterlysning.uttalelseData() != null
            && etterlysning.uttalelseData().harUttalelse();
    }

    private void autoVurder(long behandlingId, Set<DatoIntervallEntitet> ferdigePerioder) {
        if (ferdigePerioder.isEmpty()) {
            return;
        }

        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer bostedsgrunnlag for automatisk vurdering, behandlingId=" + behandlingId));

        Map<LocalDate, BostedsPeriodeAvklaring> periodeAvklaringPerFom = grunnlag.getHolder().getPeriodeAvklaringer().stream()
            .collect(Collectors.toMap(BostedsPeriodeAvklaring::getSkjæringstidspunkt, p -> p));

        Vilkårene vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer vilkårresultat for behandling " + behandlingId));

        var builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);

        vilkårene.getVilkårTimeline(VilkårType.BOSTEDSVILKÅR).stream()
            .filter(s -> s.getValue().getUtfall() != Utfall.IKKE_RELEVANT)
            .filter(s -> !s.getValue().getErManueltVurdert())
            .filter(s -> ferdigePerioder.stream().anyMatch(p -> !s.getFom().isBefore(p.getFomDato()) && !s.getTom().isAfter(p.getTomDato())))
            .forEach(s -> {
                LocalDate segmentFom = s.getFom();
                LocalDate segmentTom = s.getTom();

                var periodeAvklaring = periodeAvklaringPerFom.get(segmentFom);
                if (periodeAvklaring == null) {
                    periodeAvklaring = periodeAvklaringPerFom.entrySet().stream()
                        .filter(e -> !e.getKey().isBefore(segmentFom) && !e.getKey().isAfter(segmentTom))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
                }

                if (periodeAvklaring == null) {
                    return;
                }

                if (!periodeAvklaring.isErBosattITrondheim()) {
                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(segmentFom, segmentTom));
                    settIkkeOppfylt(periodeBuilder);
                    vilkårBuilder.leggTil(periodeBuilder);
                } else if (periodeAvklaring.getFraflyttingsDato() == null) {
                    var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(segmentFom, segmentTom));
                    periodeBuilder.medUtfall(Utfall.OPPFYLT);
                    vilkårBuilder.leggTil(periodeBuilder);
                } else {
                    LocalDate fraflyttingsDato = periodeAvklaring.getFraflyttingsDato();
                    if (!fraflyttingsDato.isAfter(segmentFom)) {
                        var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(segmentFom, segmentTom));
                        settIkkeOppfylt(periodeBuilder);
                        vilkårBuilder.leggTil(periodeBuilder);
                    } else if (fraflyttingsDato.isAfter(segmentTom)) {
                        var periodeBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(segmentFom, segmentTom));
                        periodeBuilder.medUtfall(Utfall.OPPFYLT);
                        vilkårBuilder.leggTil(periodeBuilder);
                    } else {
                        var oppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(segmentFom, fraflyttingsDato.minusDays(1)));
                        oppfyltBuilder.medUtfall(Utfall.OPPFYLT);
                        vilkårBuilder.leggTil(oppfyltBuilder);

                        var ikkeOppfyltBuilder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(fraflyttingsDato, segmentTom));
                        settIkkeOppfylt(ikkeOppfyltBuilder);
                        vilkårBuilder.leggTil(ikkeOppfyltBuilder);
                    }
                }
            });

        builder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, builder.build());
    }

    private static void settIkkeOppfylt(no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder periodeBuilder) {
        periodeBuilder.medUtfall(Utfall.IKKE_OPPFYLT)
            .medAvslagsårsak(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
    }

}
