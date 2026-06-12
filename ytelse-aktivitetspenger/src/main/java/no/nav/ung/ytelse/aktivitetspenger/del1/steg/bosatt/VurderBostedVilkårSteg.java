package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

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
import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedAvklaringOgUttalelseOgResultat.StegUtfall;

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

        var etterlysningTidslinje = new LocalDateTimeline<>(
            etterlysninger.stream().map(e->
                    new LocalDateSegment<>(e.periode().getFomDato(), e.periode().getTomDato(), e)
            ).collect(Collectors.toList())
        ).intersection(tidslinjeTilVurdering);

        var grunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer grunnlag med bostedsavklaringer"));

        var avklaringTidslinje = grunnlag.hentOppgittOgForeslåttFaktaSomTidslinje(tidslinjeTilVurdering);
        LocalDateTimeline<BostedAvklaringOgUttalelseOgResultat> vurderingTidslinje = avklaringTidslinje
            .intersection(tidslinjeTilVurdering)
            .mapValue(BostedAvklaringOgUttalelseOgResultat::new)
            .combine(
                etterlysningTidslinje,
                leggTilEtterlysning(),
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        LocalDateTimeline<StegUtfall> stegutfallTidslinje = vurderingTidslinje.mapValue(BostedAvklaringOgUttalelseOgResultat::utledUtfall);

        if (!stegutfallTidslinje.filterValue(StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER::equals).isEmpty()) {
            return settPåVent(vurderingTidslinje);
        }

        vurderingTidslinje.intersection(stegutfallTidslinje.filterValue(StegUtfall.OPPHØR_AUTOMATISK::equals))
            .toSegments()
            .forEach(s -> {
                if (s.getValue().getEtterlysning() == null) {
                    throw new IllegalStateException("Mangler etterlysning for "+ s.getLocalDateInterval());
                }

                if (!s.getValue().getEtterlysning().grunnlagsreferanse().equals(s.getValue().getAvklaring().getReferanse())) {
                    throw new IllegalStateException("Avklaring og etterlysning har ulik grunnlagsreferanse "
                        +s.getLocalDateInterval()+", "+s.getValue().getEtterlysning().grunnlagsreferanse()+", "+s.getValue().getAvklaring().getReferanse());
                }

                BostedsPeriodeAvklaring avklaring = s.getValue().getAvklaring().medNyPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()));
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

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static LocalDateSegmentCombinator<BostedAvklaringOgUttalelseOgResultat, EtterlysningData, BostedAvklaringOgUttalelseOgResultat> leggTilEtterlysning() {
        return (di, lhs, rhs) -> {
            var vurdering = lhs.getValue();
            if (rhs != null) {
                vurdering.medEtterlysning(rhs.getValue());
            }
            return new LocalDateSegment<>(di, vurdering);
        };
    }

    private static LocalDateTimeline<BostedsPeriodeAvklaring> lagAvklaringTidslinje(Collection<BostedsPeriodeAvklaring> bostedsPeriodeAvklaringer) {
        List<LocalDateSegment<BostedsPeriodeAvklaring>> segmenter = bostedsPeriodeAvklaringer.stream()
            .map(avklaring -> new LocalDateSegment<>(
                avklaring.getPeriode().getFomDato(),
                avklaring.getPeriode().getTomDato(),
                avklaring))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segmenter);
    }

    private static BehandleStegResultat settPåVent(LocalDateTimeline<BostedAvklaringOgUttalelseOgResultat> vurderingTidslinje) {
        LocalDateTime frist = vurderingTidslinje
            .filterValue(v -> v.utledUtfall() == StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER)
            .toSegments().stream()
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
}

