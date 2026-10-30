package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bistandsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
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
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
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
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste;

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
                              Vilkårsutfallsporing vilkårsutfallsporing,
                              VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste,
                              @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER) AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.vilkårsutfallsporing = vilkårsutfallsporing;
        this.vilkårsvurderingHistorikkinnslagTjeneste = vilkårsvurderingHistorikkinnslagTjeneste;
        this.aktivitetspengerVilkårsPerioderTilVurderingTjeneste = aktivitetspengerVilkårsPerioderTilVurderingTjeneste;
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

        HistorikkinnslagInput historikkinnslagInput = vilkårsvurderingHistorikkinnslagTjeneste.hentInitielleVerdier(behandlingId, getAktuellVilkårType());

        var etterlysningTidslinje = new LocalDateTimeline<>(
            etterlysninger.stream().map(e ->
                new LocalDateSegment<>(e.periode().getFomDato(), e.periode().getTomDato(), e)
            ).collect(Collectors.toList())
        ).intersection(tidslinjeTilVurdering);

        var avklaringTidslinje = hentForeslåttAvklaringTidslinje(behandlingId).intersection(tidslinjeTilVurdering);
        var nyeSøknadsperioder = aktivitetspengerVilkårsPerioderTilVurderingTjeneste.utledPerÅrsak(behandlingId, getAktuellVilkårType())
            .getOrDefault(BehandlingÅrsakType.NY_SØKT_PERIODE, LocalDateTimeline.empty())
            .intersection(tidslinjeTilVurdering);

        LocalDateTimeline<VilkårsavklaringUtfallUtleder> vurderingTidslinje = tidslinjeTilVurdering
            .combine(
                avklaringTidslinje,
                opprettUtfallUtleder(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(
                etterlysningTidslinje,
                leggTilEtterlysning(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(
                nyeSøknadsperioder,
                leggTilNySøknadsperiode(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        LocalDateTimeline<Set<VilkårsavklaringUtfall>> stegutfallTidslinje = vurderingTidslinje.mapValue(VilkårsavklaringUtfallUtleder::utledUtfall);
        vilkårsutfallsporing.lagreSporing(behandlingId, vurderingTidslinje.mapValue(VilkårsavklaringUtfallUtleder::tilRegelInput), stegutfallTidslinje, VURDER_BISTANDSVILKÅR);

        if (!stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER)).isEmpty()) {
            return settPåVent(vurderingTidslinje);
        }

        var vurderingResultat = vurderingTidslinje.intersection(stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK)))
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
                    foreslåttAvklaring.getVurdertTidspunkt(),
                    s.getValue().tilRegelsporing());
            }).collect(Collectors.toList());

        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandlingId, vurderingResultat);

        boolean vurderesEtterAvklaring = !stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.VURDERES_MANUELT_ETTER_AVKLARING)).isEmpty();
        boolean vurderesNyPeriode = !stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.VURDERES_MANUELT_NY_PERIODE)).isEmpty();
        if (vurderesEtterAvklaring && vurderesNyPeriode) {
            throw new UnsupportedOperationException("Har ikke støtte for å vurdere både opphør og ny periode i samme behandling " + behandlingId);
        }
        if (vurderesEtterAvklaring) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR_OPPHØR));
        }
        if (vurderesNyPeriode) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR));
        }

        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandlingId));
        inngangsvilkårVurderingTjeneste.settBistandsvilkårResultat(behandlingId, resultatBuilder);
        vilkårResultatRepository.lagre(behandlingId, resultatBuilder.build());

        historikkinnslagInput.setSkjermlenkeType(SkjermlenkeType.BISTANDSVILKÅR)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId, getAktuellVilkårType()))
            .setHistorikkAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDateTimeline<VilkårPeriodeAvklaring> hentForeslåttAvklaringTidslinje(long behandlingId) {
        var foreslåtteAvklaringer = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)
            .map(g -> List.copyOf(g.getForeslåtteAvklaringer()))
            .orElse(List.of());

        return new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
            .toList());
    }

    private static LocalDateSegmentCombinator<Boolean, VilkårPeriodeAvklaring, VilkårsavklaringUtfallUtleder> opprettUtfallUtleder() {
        return (di, lhs, rhs) ->
            new LocalDateSegment<>(di, new VilkårsavklaringUtfallUtleder(VilkårType.BISTANDSVILKÅR, rhs != null ? rhs.getValue() : null));
    }

    private static LocalDateSegmentCombinator<VilkårsavklaringUtfallUtleder, EtterlysningData, VilkårsavklaringUtfallUtleder> leggTilEtterlysning() {
        return (di, lhs, rhs) -> {
            var vurdering = rhs != null ? lhs.getValue().medEtterlysning(rhs.getValue()) : lhs.getValue();
            return new LocalDateSegment<>(di, vurdering);
        };
    }

    private static LocalDateSegmentCombinator<VilkårsavklaringUtfallUtleder, Boolean, VilkårsavklaringUtfallUtleder> leggTilNySøknadsperiode() {
        return (di, lhs, rhs) -> {
            var vurdering = rhs != null ? lhs.getValue().medNySøknadsperiode() : lhs.getValue();
            return new LocalDateSegment<>(di, vurdering);
        };
    }

    private static BehandleStegResultat settPåVent(LocalDateTimeline<VilkårsavklaringUtfallUtleder> vurderingTidslinje) {
        LocalDateTime frist = vurderingTidslinje
            .filterValue(v -> v.utledUtfall().contains(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER))
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
