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
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.Vilkårsutfallsporing;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
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
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfallUtleder;

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
    private Vilkårsutfallsporing vilkårsutfallsporing;

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
                              Vilkårsutfallsporing vilkårsutfallsporing) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.vilkårsutfallsporing = vilkårsutfallsporing;
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
        var perioderTilVurderingAvgrenset = avgrensTilForeslåtteAvklaringerHvisFinnes(tidslinjeTilVurdering, avklaringTidslinje);

        LocalDateTimeline<VilkårsavklaringUtfallUtleder> vurderingTidslinje = perioderTilVurderingAvgrenset
            .combine(
                avklaringTidslinje,
                leggTilAvklaring(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(
                etterlysningTidslinje,
                leggTilEtterlysning(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        LocalDateTimeline<VilkårsavklaringUtfall> stegutfallTidslinje = vurderingTidslinje.mapValue(VilkårsavklaringUtfallUtleder::utledUtfall);
        vilkårsutfallsporing.lagreSporing(behandlingId, vurderingTidslinje, stegutfallTidslinje, VURDER_BISTANDSVILKÅR.getKode());

        if (!stegutfallTidslinje.filterValue(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals).isEmpty()) {
            return settPåVent(vurderingTidslinje);
        }

        var vurderingResultat = vurderingTidslinje.intersection(stegutfallTidslinje.filterValue(VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK::equals))
            .segmenter()
            .stream().map(s -> {
                var foreslåttAvklaring = s.getValue().getForeslåttAvklaring();
                if (foreslåttAvklaring == null) {
                    throw new IllegalStateException("Foreslått avklaring mangler for periode " + s.getLocalDateInterval());
                }

                if (s.getValue().getEtterlysning() != null) {
                    if (!s.getValue().getEtterlysning().grunnlagsreferanse().equals(foreslåttAvklaring.getReferanse())) {
                        throw new IllegalStateException("Avklaring og etterlysning har ulik grunnlagsreferanse "
                            + s.getLocalDateInterval() + ", " + s.getValue().getEtterlysning().grunnlagsreferanse() + ", " + foreslåttAvklaring.getReferanse());
                    }
                }

                var ikkeOppfyltÅrsak = BistandsvilkårIkkeOppfyltÅrsak.fraKode(foreslåttAvklaring.getIkkeOppfyltÅrsakKode());
                return new BistandsvilkårResultatPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()),
                    false,
                    ikkeOppfyltÅrsak,
                    false,
                    foreslåttAvklaring.getBegrunnelse(),
                    null,
                    foreslåttAvklaring.getVurdertAv(),
                    foreslåttAvklaring.getVurdertTidspunkt());
            }).collect(Collectors.toList());

        // Kalles også med tom liste, slik at grunnlaget alltid finnes når settBistandsvilkårResultat kjører under.
        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandlingId, vurderingResultat);

        if (!stegutfallTidslinje.filterValue(v -> v == VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT).isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR));
        }

        // Hvis det kun var automatiske vurderinger og/eller tidligere vurderinger, utleder vi vilkåret automatisk basert på vurderingresultatene
        oppdaterBistandsvilkårResultatFraVurdering(behandlingId);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

     private static LocalDateTimeline<Boolean> avgrensTilForeslåtteAvklaringerHvisFinnes(
        LocalDateTimeline<Boolean> tidslinjeTilVurdering, LocalDateTimeline<VilkårPeriodeAvklaring> avklaringTidslinje) {
        return avklaringTidslinje.isEmpty() ? tidslinjeTilVurdering : tidslinjeTilVurdering.intersection(avklaringTidslinje);
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

    private static LocalDateSegmentCombinator<Boolean, VilkårPeriodeAvklaring, VilkårsavklaringUtfallUtleder> leggTilAvklaring() {
        return (di, lhs, rhs) ->
            new LocalDateSegment<>(di, new VilkårsavklaringUtfallUtleder(VilkårType.BISTANDSVILKÅR, rhs != null ? rhs.getValue() : null));
    }

    private static LocalDateSegmentCombinator<VilkårsavklaringUtfallUtleder, EtterlysningData, VilkårsavklaringUtfallUtleder> leggTilEtterlysning() {
        return (di, lhs, rhs) -> {
            var vurdering = rhs != null ? lhs.getValue().medEtterlysning(rhs.getValue()) : lhs.getValue();
            return new LocalDateSegment<>(di, vurdering);
        };
    }

    private static BehandleStegResultat settPåVent(LocalDateTimeline<VilkårsavklaringUtfallUtleder> vurderingTidslinje) {
        LocalDateTime frist = vurderingTidslinje
            .filterValue(v -> v.utledUtfall() == VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER)
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
