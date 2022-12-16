package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class GjennomgåttOpplæringMapper {

    GjennomgåttOpplæringDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<OpplæringPeriodeDto> perioder = mapPerioder(perioderFraSøknad);
        List<OpplæringVurderingDto> vurderinger = mapVurderinger(grunnlag, perioder);
        return new GjennomgåttOpplæringDto(perioder, vurderinger);
    }

    private List<OpplæringPeriodeDto> mapPerioder(Set<PerioderFraSøknad> perioderFraSøknad) {
        List<OpplæringPeriodeDto> perioder = new ArrayList<>();

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            for (KursPeriode kursPeriode : fraSøknad.getKurs()) {
                perioder.add(new OpplæringPeriodeDto(kursPeriode.getPeriode().tilPeriode()));
            }
        }

        return perioder;
    }

    private List<OpplæringVurderingDto> mapVurderinger(VurdertOpplæringGrunnlag grunnlag, List<OpplæringPeriodeDto> perioder) {
        List<OpplæringVurderingDto> vurderinger = new ArrayList<>();

        if (grunnlag != null && grunnlag.getVurdertePerioder() != null) {
            for (VurdertOpplæringPeriode vurdertOpplæringPeriode : grunnlag.getVurdertePerioder().getPerioder()) {
                vurderinger.add(new OpplæringVurderingDto(vurdertOpplæringPeriode.getPeriode().tilPeriode(),
                    vurdertOpplæringPeriode.getGjennomførtOpplæring() ? Resultat.GODKJENT : Resultat.IKKE_GODKJENT,
                    vurdertOpplæringPeriode.getBegrunnelse())
                );
            }
        }

        LocalDateTimeline<Boolean> tidslinjeTilVurdering = new LocalDateTimeline<>(perioder.stream()
            .map(OpplæringPeriodeDto::getPeriode)
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());

        LocalDateTimeline<Boolean> tidslinjeMedVurdering = new LocalDateTimeline<>(vurderinger.stream()
            .map(OpplæringVurderingDto::getPeriode)
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());

        LocalDateTimeline<Boolean> tidslinjeSomManglerVurdering = tidslinjeTilVurdering.disjoint(tidslinjeMedVurdering);

        tidslinjeSomManglerVurdering.forEach(segment -> vurderinger.add(new OpplæringVurderingDto(
            new Periode(segment.getFom(), segment.getTom()),
            Resultat.MÅ_VURDERES,
            null))
        );

        return vurderinger;
    }
}
