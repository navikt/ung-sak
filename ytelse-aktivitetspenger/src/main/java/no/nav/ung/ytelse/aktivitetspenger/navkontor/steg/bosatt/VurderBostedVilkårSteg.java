package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt;

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
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandlingprosessSporing;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfallUtleder;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.HistorikkinnslagInput;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
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
    private static final Logger logger = LoggerFactory.getLogger(VurderBostedVilkårSteg.class);

    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private EtterlysningTjeneste etterlysningTjeneste;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private BehandingprosessSporingRepository behandingprosessSporingRepository;
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste;


    VurderBostedVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderBostedVilkårSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                  VilkårResultatRepository vilkårResultatRepository,
                                  VilkårTjeneste vilkårTjeneste,
                                  BehandlingRepository behandlingRepository,
                                  VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                  @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                  EtterlysningTjeneste etterlysningTjeneste,
                                  InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                  InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste,
                                  BehandingprosessSporingRepository behandingprosessSporingRepository,
                                  VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste,
                                  @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER) AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
        this.behandingprosessSporingRepository = behandingprosessSporingRepository;
        this.vilkårsvurderingHistorikkinnslagTjeneste = vilkårsvurderingHistorikkinnslagTjeneste;
        this.aktivitetspengerVilkårsPerioderTilVurderingTjeneste = aktivitetspengerVilkårsPerioderTilVurderingTjeneste;
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

        List<EtterlysningData> etterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingId, kontekst.getFagsakId(), EtterlysningType.UTTALELSE_BOSTED);
        var etterlysningTidslinje = new LocalDateTimeline<>(etterlysninger.stream().map(e -> new LocalDateSegment<>(e.periode().getFomDato(), e.periode().getTomDato(), e)).toList())
            .intersection(tidslinjeTilVurdering);

        var foreslåtteAvklaringer = vilkårsavklaringGrunnlagRepository
            .hentGrunnlagHvisEksisterer(behandlingId, getAktuellVilkårType())
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());

        HistorikkinnslagInput historikkinnslagInput = vilkårsvurderingHistorikkinnslagTjeneste.hentInitielleVerdier(behandlingId, getAktuellVilkårType());

        var avklaringTidslinje = new LocalDateTimeline<>(foreslåtteAvklaringer.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
            .toList())
            .intersection(tidslinjeTilVurdering);

        var nyeSøknadsperioder = aktivitetspengerVilkårsPerioderTilVurderingTjeneste.utledPerÅrsak(behandlingId, getAktuellVilkårType())
            .getOrDefault(BehandlingÅrsakType.NY_SØKT_PERIODE, LocalDateTimeline.empty())
            .intersection(tidslinjeTilVurdering);

        LocalDateTimeline<VilkårsavklaringUtfallUtleder> vurderingTidslinje = tidslinjeTilVurdering
            .combine(avklaringTidslinje, opprettUtfallUtleder(), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(etterlysningTidslinje, leggTilEtterlysning(), LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(nyeSøknadsperioder, leggTilNySøknadsperiode(), LocalDateTimeline.JoinStyle.LEFT_JOIN);

        LocalDateTimeline<Set<VilkårsavklaringUtfall>> stegutfallTidslinje = vurderingTidslinje.mapValue(VilkårsavklaringUtfallUtleder::utledUtfall);
        lagreBehandlingprosessSporing(behandlingId, vurderingTidslinje, stegutfallTidslinje);

        if (!stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER)).isEmpty()) {
            return settPåVent(vurderingTidslinje);
        }

        var vurderingResultat = vurderingTidslinje.intersection(stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK)))
            .segmenter()
            .stream().map(s -> {
                if (s.getValue().getEtterlysning() != null) {
                    if (!s.getValue().getEtterlysning().grunnlagsreferanse().equals(s.getValue().getForeslåttAvklaring().getReferanse())) {
                        throw new IllegalStateException("Avklaring og etterlysning har ulik grunnlagsreferanse "
                            + s.getLocalDateInterval() + ", " + s.getValue().getEtterlysning().grunnlagsreferanse() + ", " + s.getValue().getForeslåttAvklaring().getReferanse());
                    }
                }

                var foreslåttAvklaring = s.getValue().getForeslåttAvklaring();
                return new BostedsvilkårResultatPeriode(
                    DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()),
                    false,
                    BostedsvilkårIkkeOppfyltÅrsak.fraKode(foreslåttAvklaring.getIkkeOppfyltÅrsakKode()),
                    false,
                    null,
                    null,
                    null,
                    foreslåttAvklaring.getVurdertTidspunkt(),
                    s.getValue().tilRegelsporing()
                );
            }).collect(Collectors.toList());

        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandlingId, vurderingResultat);

        if (!stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.VURDERES_MANUELT_ETTER_AVKLARING)).isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BOSTEDSVILKÅR_OPPHØR));
        }
        if (!stegutfallTidslinje.filterValue(u -> u.contains(VilkårsavklaringUtfall.VURDERES_MANUELT_NY_PERIODE)).isEmpty()) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR));
        }

        // Hvis det kun var automatiske vurderinger og/eller tidligere vurderinger, utleder vi vilkåret automatisk basert på vurderingresultatene
        inngangsvilkårVurderingTjeneste.oppdaterVilkårResultatFraVurdering(behandlingId, getAktuellVilkårType());

        //lag historikkinnslag for endringer gjort automatisk
        historikkinnslagInput.setSkjermlenkeType(SkjermlenkeType.BOSTEDSVILKÅR)
            .setNyeVilkårVurderinger(inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandlingId, getAktuellVilkårType()))
            .setHistorikkAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        vilkårsvurderingHistorikkinnslagTjeneste.lagreHistorikkinnslag(historikkinnslagInput);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void lagreBehandlingprosessSporing(long behandlingId, LocalDateTimeline<VilkårsavklaringUtfallUtleder> vurderingTidslinje, LocalDateTimeline<Set<VilkårsavklaringUtfall>> stegutfallTidslinje) {
        try {
            behandingprosessSporingRepository.lagreSporing(new BehandlingprosessSporing(
                behandlingId,
                JsonObjectMapper.getJson(vurderingTidslinje.mapValue(VilkårsavklaringUtfallUtleder::tilRegelInput)),
                JsonObjectMapper.getJson(stegutfallTidslinje),
                VURDER_BOSTEDVILKÅR.getKode())
            );
        } catch (IOException e) {
            logger.warn("Feil ved lagring av sporing for utledning av StegUtfall i VurderBostedVilkårSteg", e);
        }
    }

    private static LocalDateSegmentCombinator<VilkårsavklaringUtfallUtleder, EtterlysningData, VilkårsavklaringUtfallUtleder> leggTilEtterlysning() {
        return (di, lhs, rhs) -> {
            var vurdering = rhs != null ? lhs.getValue().medEtterlysning(rhs.getValue()) : lhs.getValue();
            return new LocalDateSegment<>(di, vurdering);
        };
    }

    private static LocalDateSegmentCombinator<Boolean, VilkårPeriodeAvklaring, VilkårsavklaringUtfallUtleder> opprettUtfallUtleder() {
        return (di, lhs, rhs) ->
            new LocalDateSegment<>(di, new VilkårsavklaringUtfallUtleder(VilkårType.BOSTEDSVILKÅR, rhs != null ? rhs.getValue() : null));
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
                EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon(),
                EtterlysningType.UTTALELSE_BOSTED.mapTilVenteårsak(),
                frist
            )
        ));
    }
}

