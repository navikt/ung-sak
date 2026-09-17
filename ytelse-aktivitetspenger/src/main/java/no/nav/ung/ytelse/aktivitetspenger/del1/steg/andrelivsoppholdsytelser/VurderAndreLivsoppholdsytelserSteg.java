package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

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
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.Vilkårsutfallsporing;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatPeriode;
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
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfallUtleder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderAndreLivsoppholdsytelserSteg extends VilkårVurderingSteg {

    private static final Duration DEFAULT_VENTEFRIST = Duration.ofDays(14);
    private static final EtterlysningType ETTERLYSNING_TYPE = EtterlysningType.UTTALELSE_ANDRE_LIVSOPPHOLDSYTELSER;

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private EtterlysningTjeneste etterlysningTjeneste;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private Vilkårsutfallsporing vilkårsutfallsporing;
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    VurderAndreLivsoppholdsytelserSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderAndreLivsoppholdsytelserSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                              VilkårResultatRepository vilkårResultatRepository,
                                              VilkårTjeneste vilkårTjeneste,
                                              BehandlingRepository behandlingRepository,
                                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                              EtterlysningTjeneste etterlysningTjeneste,
                                              VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                              InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                              InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                              Vilkårsutfallsporing vilkårsutfallsporing,
                                              VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.vilkårsutfallsporing = vilkårsutfallsporing;
        this.vilkårsvurderingHistorikkinnslagTjeneste = vilkårsvurderingHistorikkinnslagTjeneste;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
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
            behandlingId, kontekst.getFagsakId(), ETTERLYSNING_TYPE);

        HistorikkinnslagInput historikkinnslagInput = vilkårsvurderingHistorikkinnslagTjeneste.hentInitielleVerdier(behandlingId, getAktuellVilkårType());

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
                opprettUtfallUtleder(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(
                etterlysningTidslinje,
                leggTilEtterlysning(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        LocalDateTimeline<VilkårsavklaringUtfall> stegutfallTidslinje = vurderingTidslinje.mapValue(VilkårsavklaringUtfallUtleder::utledUtfall);
        vilkårsutfallsporing.lagreSporing(behandlingId, vurderingTidslinje, stegutfallTidslinje, VURDER_ANDRE_LIVSOPPHOLDSYTELSER);

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

                var ikkeOppfyltÅrsak = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.fraKode(foreslåttAvklaring.getIkkeOppfyltÅrsakKode());
                return new AndreLivsoppholdsytelserResultatPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()),
                    false,
                    ikkeOppfyltÅrsak,
                    false,
                    foreslåttAvklaring.getBegrunnelse(),
                    null,
                    foreslåttAvklaring.getVurdertAv(),
                    foreslåttAvklaring.getVurdertTidspunkt());
            }).collect(Collectors.toList());

        // Kalles også med tom liste, slik at grunnlaget alltid finnes når settAndreLivsoppholdsytelserResultat kjører under.
        inngangsvilkårVurderingRepository.lagreYtelseVurderinger(behandlingId, vurderingResultat);

        if (!stegutfallTidslinje.filterValue(v -> v == VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT).isEmpty()) {
            var manuellVurderingTidslinje = vurderingTidslinje.intersection(stegutfallTidslinje.filterValue(v -> v == VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT));
            var aksjonspunkt = erDekketAvForeslåttAvklaring(manuellVurderingTidslinje)
                ? AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER_OPPHØR
                : AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER;
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunkt));
        }

        // Hvis det kun var automatiske vurderinger og/eller tidligere vurderinger, utleder vi vilkåret automatisk basert på vurderingresultatene
        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandlingId));
        inngangsvilkårVurderingTjeneste.settAndreLivsoppholdsytelserResultat(behandlingId, resultatBuilder);
        vilkårResultatRepository.lagre(behandlingId, resultatBuilder.build());

        historikkinnslagInput.setSkjermlenkeType(SkjermlenkeType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId, getAktuellVilkårType()))
            .setHistorikkAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    static boolean erDekketAvForeslåttAvklaring(LocalDateTimeline<VilkårsavklaringUtfallUtleder> manuellTidslinje) {
        return manuellTidslinje.segmenter().stream()
            .allMatch(s -> s.getValue().getForeslåttAvklaring() != null);
    }

    private static LocalDateTimeline<Boolean> avgrensTilForeslåtteAvklaringerHvisFinnes(
        LocalDateTimeline<Boolean> tidslinjeTilVurdering, LocalDateTimeline<VilkårPeriodeAvklaring> avklaringTidslinje) {
        return avklaringTidslinje.isEmpty() ? tidslinjeTilVurdering : tidslinjeTilVurdering.intersection(avklaringTidslinje);
    }

    private LocalDateTimeline<VilkårPeriodeAvklaring> hentForeslåttAvklaringTidslinje(long behandlingId) {
        var foreslåtteAvklaringer = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR)
            .map(g -> List.copyOf(g.getForeslåtteAvklaringer()))
            .orElse(List.of());

        return new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
            .toList());
    }

    private static LocalDateSegmentCombinator<Boolean, VilkårPeriodeAvklaring, VilkårsavklaringUtfallUtleder> opprettUtfallUtleder() {
        return (di, lhs, rhs) ->
            new LocalDateSegment<>(di, new VilkårsavklaringUtfallUtleder(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, rhs != null ? rhs.getValue() : null));
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
                ETTERLYSNING_TYPE.tilAutopunktDefinisjon(),
                ETTERLYSNING_TYPE.mapTilVenteårsak(),
                frist
            )
        ));
    }

}
