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
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaringHolder;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
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

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BOSTEDVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BOSTEDVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderBostedVilkårSteg extends VilkårVurderingSteg {

    private static final Duration DEFAULT_VENTEFRIST = Duration.ofDays(14);

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private EtterlysningTjeneste etterlysningTjeneste;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;

    VurderBostedVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderBostedVilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                  VilkårResultatRepository vilkårResultatRepository,
                                  VilkårTjeneste vilkårTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  BostedsGrunnlagRepository bostedsGrunnlagRepository,
                                  @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                  EtterlysningTjeneste etterlysningTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
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

        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer grunnlag med bostedsavklaringer"));
        BostedsAvklaringHolder bostedAvklaring = grunnlag.getForeslått();

        LocalDateTimeline<StegUtfall> stegutfallTidslinje = tidslinjeTilVurdering.map(
            segment -> vurder(segment, etterlysningPerFom, bostedAvklaring));

        if (!stegutfallTidslinje.filterValue(StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals).isEmpty()) {
            return settPåVent(stegutfallTidslinje, etterlysningPerFom);
        }

        stegutfallTidslinje.filterValue(StegUtfall.OPPHØR_AUTOMATISK::equals)
            .toSegments()
            .forEach(s -> {
                BostedsPeriodeAvklaring avklaring = bostedAvklaring.getPeriodeAvklaring(s.getFom())
                    .orElseThrow(() -> new IllegalStateException("Forventer bostedsavklaring for stp " + s.getFom()));
                Avslagsårsak avslagsårsak = mapTilAvslagsårsak(avklaring.getFraflyttingsÅrsak());
//                vurdertAktivitetspengerGrunnlag.lagre(new BostedsvurderingResultat(
//                    behandlingId,
//                    s.getFom(),
//                    avklaring.getPeriode().getFomDato(),
//                    avslagsårsak,
//                    OpphørKilde.AUTOMATISK,
//                    VilkårType.BOSTEDSVILKÅR,
//                    null,
//                    null));
            });
//        opphørTjeneste.utledOgLagreVilkår(behandlingId, VilkårType.BOSTEDSVILKÅR, tidslinjeTilVurdering);

        if (!stegutfallTidslinje.filterValue(StegUtfall.VILKÅR_VURDERES_MANUELT::equals).isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR));
        }

        if (!stegutfallTidslinje.filterValue(StegUtfall.OPPHØR_MANUELT::equals).isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_OPPHØR_BOSTED));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static List<LocalDateSegment<StegUtfall>> vurder(LocalDateSegment<Boolean> segment,
                                                              Map<LocalDate, EtterlysningData> etterlysningPerFom,
                                                              BostedsAvklaringHolder holder) {
        LocalDate fom = segment.getFom();
        EtterlysningData etterlysning = etterlysningPerFom.get(fom);
        BostedsPeriodeAvklaring avklaring = holder.getPeriodeAvklaring(fom)
            .orElseThrow(() -> new IllegalStateException("Forventer å finne en bostedsperiodeavklaring for stp " + fom));
        boolean erÅrsakAnnet = FraflyttingsÅrsak.ANNET.equals(avklaring.getFraflyttingsÅrsak());
        boolean erKildeSøknad = Kilde.SØKNAD.equals(avklaring.getKilde());

        if (erVentende(etterlysning)) {
            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER));
        } else if (erKildeSøknad) {
            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.VILKÅR_VURDERES_MANUELT));
        } else if (harMottattSvarMedUttalelse(etterlysning) || erÅrsakAnnet) {
            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.OPPHØR_MANUELT));
        } else if (!avklaring.isErBosattITrondheim()) {
            return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.OPPHØR_AUTOMATISK));
        }
        return List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), StegUtfall.BOSATT_HELE_PERIODEN));
    }

    private static BehandleStegResultat settPåVent(LocalDateTimeline<StegUtfall> stegutfallTidslinje,
                                                   Map<LocalDate, EtterlysningData> etterlysningPerFom) {
        Set<LocalDate> ventendeFom = stegutfallTidslinje.filterValue(StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals)
            .toSegments().stream().map(LocalDateSegment::getFom).collect(Collectors.toSet());
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

    private static boolean erVentende(EtterlysningData etterlysning) {
        return etterlysning != null
            && (etterlysning.status() == EtterlysningStatus.OPPRETTET
            || etterlysning.status() == EtterlysningStatus.VENTER);
    }

    private static boolean harMottattSvarMedUttalelse(EtterlysningData etterlysning) {
        return etterlysning != null
            && etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
            && etterlysning.uttalelseData() != null
            && etterlysning.uttalelseData().harUttalelse();
    }

    static Avslagsårsak mapTilAvslagsårsak(FraflyttingsÅrsak fraflyttingsÅrsak) {
        if (fraflyttingsÅrsak == null) {
            return Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED;
        }
        return switch (fraflyttingsÅrsak) {
            case IKKE_BOSATTADRESSE_I_TRONDHEIM ->
                Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED;
            case IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM ->
                Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE;
            case STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM ->
                Avslagsårsak.YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED;
            case ANNET ->
                throw new IllegalStateException("FraflyttingsÅrsak.ANNET skal ikke treffe auto-path");
        };
    }

    enum StegUtfall {
        OPPHØR_AUTOMATISK,
        OPPHØR_MANUELT,
        VILKÅR_VURDERES_MANUELT,
        VENTER_PÅ_UTTALELSE_FRA_BRUKER,
        BOSATT_HELE_PERIODEN
    }
}

