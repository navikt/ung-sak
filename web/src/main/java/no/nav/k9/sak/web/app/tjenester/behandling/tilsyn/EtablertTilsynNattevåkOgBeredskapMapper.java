package no.nav.k9.sak.web.app.tjenester.behandling.tilsyn;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.tilsyn.*;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynBeskrivelse;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn.UnntakEtablertTilsynPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.tilsyn.MapTilsyn;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PSBVilkårsPerioderTilVurderingTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class EtablertTilsynNattevåkOgBeredskapMapper {

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public EtablertTilsynNattevåkOgBeredskapMapper(@BehandlingTypeRef PSBVilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                                   @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste) {
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public EtablertTilsynNattevåkOgBeredskapDto tilDto(BehandlingReferanse behandlingRef,
                                                       UttaksPerioderGrunnlag uttaksPerioderGrunnlag,
                                                       UnntakEtablertTilsynGrunnlag unntakEtablertTilsynGrunnlag) {
        var beredskap = unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getBeredskap();
        var nattevåk = unntakEtablertTilsynGrunnlag.getUnntakEtablertTilsynForPleietrengende().getNattevåk();

        return new EtablertTilsynNattevåkOgBeredskapDto(
            tilEtablertTilsyn(behandlingRef, uttaksPerioderGrunnlag),
            tilNattevåk(nattevåk, behandlingRef.getAktørId()),
            tilBeredskap(beredskap, behandlingRef.getAktørId())
        );
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

    private NattevåkDto tilNattevåk(UnntakEtablertTilsyn nattevåk, AktørId søkersAktørId) {
        return new NattevåkDto(tilBeskrivelser(nattevåk.getBeskrivelser(), søkersAktørId), tilVurderinger(nattevåk.getPerioder(), søkersAktørId));
    }

    private BeredskapDto tilBeredskap(UnntakEtablertTilsyn beredskap, AktørId søkersAktørId) {
        return new BeredskapDto(tilBeskrivelser(beredskap.getBeskrivelser(), søkersAktørId), tilVurderinger(beredskap.getPerioder(), søkersAktørId));
    }

    private List<BeskrivelseDto> tilBeskrivelser(List<UnntakEtablertTilsynBeskrivelse> uetBeskrivelser, AktørId søkersAktørId) {
        return uetBeskrivelser.stream().map(uetBeskrivelse ->
            new BeskrivelseDto(
                new Periode(uetBeskrivelse.getPeriode().getFomDato(), uetBeskrivelse.getPeriode().getTomDato()),
                uetBeskrivelse.getTekst(),
                uetBeskrivelse.getMottattDato(),
                tilKilde(søkersAktørId, uetBeskrivelse.getSøker())
            )
        ).toList();
    }

    private List<VurderingDto> tilVurderinger(List<UnntakEtablertTilsynPeriode> uetPerioder, AktørId søkersAktørId) {
        return uetPerioder.stream().map(uetPeriode ->
            new VurderingDto(
                uetPeriode.getId(),
                new Periode(uetPeriode.getPeriode().getFomDato(), uetPeriode.getPeriode().getTomDato()),
                uetPeriode.getBegrunnelse(),
                uetPeriode.getResultat(),
                tilKilde(søkersAktørId, uetPeriode.getAktørId())
            )
        ).toList();
    }

    private Kilde tilKilde(AktørId periodeAktørId, AktørId søkersAktørId) {
        if (Objects.equals(periodeAktørId, søkersAktørId)) {
            return Kilde.SØKER;
        }
        return Kilde.ANDRE;
    }

}
