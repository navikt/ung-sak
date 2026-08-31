package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.AvklaringSporing;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår.BistandAvklaringOgUttalelseOgResultat.StegUtfall;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_BISTANDSVILKÅR;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_BISTANDSVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class BistandsvilkårSteg extends VilkårVurderingSteg {

    private static final Duration DEFAULT_VENTEFRIST = Duration.ofDays(14);

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private AvklaringSporing avklaringSporing;

    BistandsvilkårSteg() {
        //for CDI proxy
    }

    @Inject
    public BistandsvilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                              VilkårResultatRepository vilkårResultatRepository,
                              VilkårTjeneste vilkårTjeneste,
                              BehandlingRepository behandlingRepository,
                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                              EtterlysningTjeneste etterlysningTjeneste,
                              VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                              InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                              InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                              AvklaringSporing avklaringSporing) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.avklaringSporing = avklaringSporing;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.BISTANDSVILKÅR;
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
            behandlingId, kontekst.getFagsakId(), EtterlysningType.UTTALELSE_BISTAND);

        var etterlysningTidslinje = new LocalDateTimeline<>(
            etterlysninger.stream().map(e ->
                new LocalDateSegment<>(e.periode().getFomDato(), e.periode().getTomDato(), e)
            ).collect(Collectors.toList())
        ).intersection(tidslinjeTilVurdering);

        var avklaringTidslinje = hentForeslåttAvklaringTidslinje(behandlingId).intersection(tidslinjeTilVurdering);

        var tidligereVilkårVurderingResultat = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId)
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBistandTidslinje)
            .orElse(new LocalDateTimeline<>(List.of()));

        // I motsetning til bosted finnes det ingen faktatidslinje som dekker hele vilkårsperioden. Vi bygger derfor
        // tidslinjen fra periodene til vurdering (LEFT_JOIN), slik at perioder uten foreslått avklaring fortsatt
        // ender i manuell vurdering — som er dagens oppførsel for bistandsvilkåret.
        LocalDateTimeline<BistandAvklaringOgUttalelseOgResultat> vurderingTidslinje = tidslinjeTilVurdering
            .combine(
                avklaringTidslinje,
                leggTilAvklaring(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(
                etterlysningTidslinje,
                leggTilEtterlysning(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(
                tidligereVilkårVurderingResultat,
                leggTilResultat(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        LocalDateTimeline<StegUtfall> stegutfallTidslinje = vurderingTidslinje.mapValue(BistandAvklaringOgUttalelseOgResultat::utledUtfall);
        avklaringSporing.lagreSporing(behandlingId, vurderingTidslinje, stegutfallTidslinje, VURDER_BISTANDSVILKÅR.getKode());

        if (!stegutfallTidslinje.filterValue(StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals).isEmpty()) {
            return settPåVent(vurderingTidslinje);
        }

        var vurderingResultat = vurderingTidslinje.intersection(stegutfallTidslinje.filterValue(StegUtfall.AVSLAG_AUTOMATISK::equals))
            .segmenter()
            .stream().map(s -> {
                var foreslåttAvklaring = s.getValue().getForeslåttAvklaring();
                if (s.getValue().getEtterlysning() != null) {
                    if (!s.getValue().getEtterlysning().grunnlagsreferanse().equals(foreslåttAvklaring.getReferanse())) {
                        throw new IllegalStateException("Avklaring og etterlysning har ulik grunnlagsreferanse "
                            + s.getLocalDateInterval() + ", " + s.getValue().getEtterlysning().grunnlagsreferanse() + ", " + foreslåttAvklaring.getReferanse());
                    }
                }

                return new BistandsvilkårResultatPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()),
                    false,
                    s.getValue().getIkkeOppfyltÅrsak(),
                    false,
                    foreslåttAvklaring.getBegrunnelse(),
                    null,
                    foreslåttAvklaring.getVurdertAv(),
                    foreslåttAvklaring.getVurdertTidspunkt());
            }).collect(Collectors.toList());

        // Kalles også med tom liste, slik at grunnlaget alltid finnes når settBistandsvilkårResultat kjører under.
        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandlingId, vurderingResultat);

        if (!stegutfallTidslinje.filterValue(StegUtfall.VILKÅR_VURDERES_MANUELT::equals).isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR));
        }

        // Hvis det kun var automatiske vurderinger og/eller tidligere vurderinger, utleder vi vilkåret automatisk basert på vurderingresultatene
        oppdaterBistandsvilkårResultatFraVurdering(behandlingId);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void oppdaterBistandsvilkårResultatFraVurdering(long behandlingId) {
        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandlingId));
        inngangsvilkårVurderingTjeneste.settBistandsvilkårResultat(behandlingId, resultatBuilder);
        vilkårResultatRepository.lagre(behandlingId, resultatBuilder.build());
    }

    private LocalDateTimeline<VilkårPeriodeAvklaring> hentForeslåttAvklaringTidslinje(long behandlingId) {
        var foreslåtteAvklaringer = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(g -> List.copyOf(g.getForeslåtteAvklaringer()))
            .orElse(List.of());

        return new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
            .toList());
    }

    private static LocalDateSegmentCombinator<Boolean, VilkårPeriodeAvklaring, BistandAvklaringOgUttalelseOgResultat> leggTilAvklaring() {
        return (di, lhs, rhs) -> new LocalDateSegment<>(di, new BistandAvklaringOgUttalelseOgResultat(rhs != null ? rhs.getValue() : null));
    }

    private static LocalDateSegmentCombinator<BistandAvklaringOgUttalelseOgResultat, EtterlysningData, BistandAvklaringOgUttalelseOgResultat> leggTilEtterlysning() {
        return (di, lhs, rhs) -> {
            var vurdering = rhs != null ? lhs.getValue().medEtterlysning(rhs.getValue()) : lhs.getValue();
            return new LocalDateSegment<>(di, vurdering);
        };
    }

    private static LocalDateSegmentCombinator<BistandAvklaringOgUttalelseOgResultat, VilkårsvurderingResultat, BistandAvklaringOgUttalelseOgResultat> leggTilResultat() {
        return (di, lhs, rhs) -> {
            var vurdering = rhs != null ? lhs.getValue().medResultat(rhs.getValue()) : lhs.getValue();
            return new LocalDateSegment<>(di, vurdering);
        };
    }

    private static BehandleStegResultat settPåVent(LocalDateTimeline<BistandAvklaringOgUttalelseOgResultat> vurderingTidslinje) {
        LocalDateTime frist = vurderingTidslinje
            .filterValue(v -> v.utledUtfall() == StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER)
            .segmenter().stream()
            .map(seg -> seg.getValue().getFrist())
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(LocalDateTime.now().plus(DEFAULT_VENTEFRIST));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(
            AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                EtterlysningType.UTTALELSE_BISTAND.tilAutopunktDefinisjon(),
                EtterlysningType.UTTALELSE_BISTAND.mapTilVenteårsak(),
                frist
            )
        ));
    }

}
