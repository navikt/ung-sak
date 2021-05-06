package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.tilsyn.*;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsynBeskrivelse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn.UnntakEtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn.MapTilsyn;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

@Dependent
public class EtablertTilsynNattevåkOgBeredskapMapper {

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;

    @Inject
    public EtablertTilsynNattevåkOgBeredskapMapper(@FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                   @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public EtablertTilsynNattevåkOgBeredskapDto tilDto(BehandlingReferanse behandlingRef,
                                                       UttaksPerioderGrunnlag uttaksPerioderGrunnlag,
                                                       UnntakEtablertTilsynGrunnlag unntakEtablertTilsynGrunnlag) {
        var beredskap = unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        var nattevåk = unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk();

        return new EtablertTilsynNattevåkOgBeredskapDto(tilEtablertTilsyn(behandlingRef, uttaksPerioderGrunnlag), tilNattevåk(nattevåk), tilBeredskap(beredskap));
    }

    private List<EtablertTilsynPeriodeDto> tilEtablertTilsyn(BehandlingReferanse behandlingRef, UttaksPerioderGrunnlag uttaksPerioderGrunnlag) {
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(behandlingRef);
        var kravDokumenter = vurderteSøknadsperioder.keySet();

        var perioderFraSøknader = uttaksPerioderGrunnlag.getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene();

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandlingRef.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var utvidetRevurderingPerioder = perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(behandlingRef);

        var tidslinjeTilVurdering = new LocalDateTimeline<>(mapPerioderTilVurdering(perioderTilVurdering, utvidetRevurderingPerioder));
        final Map<LukketPeriode, Duration> tilsynsperioder = new MapTilsyn().map(kravDokumenter, perioderFraSøknader, tidslinjeTilVurdering);

        return tilsynsperioder.entrySet().stream().map(entry -> new EtablertTilsynPeriodeDto(new Periode(entry.getKey().getFom(), entry.getKey().getTom()), entry.getValue())).toList();
    }


    private List<LocalDateSegment<Boolean>> mapPerioderTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, NavigableSet<DatoIntervallEntitet> utvidetRevurderingPerioder) {

        var timeline = new LocalDateTimeline<>(perioderTilVurdering
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));

        var utvidetePerioder = new LocalDateTimeline<>(utvidetRevurderingPerioder
            .stream()
            .map(it -> new LocalDateSegment<>(it.getFomDato(), it.getTomDato(), true))
            .collect(Collectors.toList()));
        timeline = timeline.combine(utvidetePerioder, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return new ArrayList<>(timeline.toSegments());
    }

    private NattevåkDto tilNattevåk(UnntakEtablertTilsyn nattevåk) {
        return new NattevåkDto(tilBeskrivelser(nattevåk.getBeskrivelser()), tilVurderinger(nattevåk.getPerioder()));
    }
    private BeredskapDto tilBeredskap(UnntakEtablertTilsyn beredskap) {
        return new BeredskapDto(tilBeskrivelser(beredskap.getBeskrivelser()), tilVurderinger(beredskap.getPerioder()));
    }

    private List<BeskrivelseDto> tilBeskrivelser(List<UnntakEtablertTilsynBeskrivelse> uetBeskrivelser) {
        return uetBeskrivelser.stream().map(uetBeskrivelse ->
            new BeskrivelseDto(
                new Periode(uetBeskrivelse.getPeriode().getFomDato(), uetBeskrivelse.getPeriode().getTomDato()),
                uetBeskrivelse.getTekst(),
                uetBeskrivelse.getMottattDato(),
                Kilde.SØKER //TODO utled dette
            )
        ).toList();
    }

    private List<VurderingDto> tilVurderinger(List<UnntakEtablertTilsynPeriode> uetPerioder) {
        return uetPerioder.stream().map(uetPeriode ->
            new VurderingDto(
                uetPeriode.getId(),
                new Periode(uetPeriode.getPeriode().getFomDato(), uetPeriode.getPeriode().getTomDato()),
                uetPeriode.getBegrunnelse(),
                uetPeriode.getResultat(),
                Kilde.SØKER //TODO utled dette
            )
        ).toList();
    }

}
