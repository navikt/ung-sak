package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class GjennomgåttOpplæringMapper {

    GjennomgåttOpplæringDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<OpplæringPeriodeDto> perioder = mapPerioder(perioderFraSøknad);
        List<OpplæringVurderingDto> vurderinger = mapVurderinger(grunnlag, perioder);
        boolean trengerVurderingAvReisetid = trengerVurderingAvReisetid(grunnlag, perioder);
        return new GjennomgåttOpplæringDto(perioder, vurderinger, trengerVurderingAvReisetid);
    }

    private List<OpplæringPeriodeDto> mapPerioder(Set<PerioderFraSøknad> perioderFraSøknad) {
        List<OpplæringPeriodeDto> perioder = new ArrayList<>();

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            for (KursPeriode kursPeriode : fraSøknad.getKurs()) {
                perioder.add(new OpplæringPeriodeDto(
                    kursPeriode.getPeriode().tilPeriode(),
                    mapReisetid(kursPeriode))
                );
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
                    vurdertOpplæringPeriode.getBegrunnelse(),
                    mapReisetidVurdering(vurdertOpplæringPeriode))
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
            null,
            null))
        );

        return vurderinger;
    }

    private ReisetidDto mapReisetid(KursPeriode kursPeriode) {
        Periode reisetidTil = kursPeriode.getReiseperiodeTil() != null ? kursPeriode.getReiseperiodeTil().tilPeriode() : null;
        Periode reisetidHjem = kursPeriode.getReiseperiodeHjem() != null ? kursPeriode.getReiseperiodeHjem().tilPeriode() : null;
        return new ReisetidDto(reisetidTil, reisetidHjem);
    }

    private ReisetidVurderingDto mapReisetidVurdering(VurdertOpplæringPeriode vurdertOpplæringPeriode) {
        VurdertReisetid vurdertReisetid = vurdertOpplæringPeriode.getReisetid();
        if (vurdertReisetid != null) {
            Periode reisetidTil = vurdertReisetid.getReiseperiodeTil() != null ? vurdertReisetid.getReiseperiodeTil().tilPeriode() : null;
            Periode reisetidHjem = vurdertReisetid.getReiseperiodeHjem() != null ? vurdertReisetid.getReiseperiodeHjem().tilPeriode() : null;
            return new ReisetidVurderingDto(reisetidTil, reisetidHjem, vurdertReisetid.getBegrunnelse());
        }

        return null;
    }

    private boolean trengerVurderingAvReisetid(VurdertOpplæringGrunnlag grunnlag, List<OpplæringPeriodeDto> perioder) {
        var tidslinjeMedGodkjentReisetid = lagTidslinjeMedGodkjentReisetid(grunnlag, perioder);
        var reisetidSomSkalGodkjennes = lagTidslinjeMedReisedagerSomSkalGodkjennes(perioder);
        return !reisetidSomSkalGodkjennes.disjoint(tidslinjeMedGodkjentReisetid).isEmpty();
    }

    private LocalDateTimeline<Boolean> lagTidslinjeMedGodkjentReisetid(VurdertOpplæringGrunnlag grunnlag, List<OpplæringPeriodeDto> perioder) {

        LocalDateTimeline<Boolean> vurdert = LocalDateTimeline.empty();

        if (grunnlag != null && grunnlag.getVurdertePerioder() != null) {
            for (VurdertOpplæringPeriode vurdertOpplæringPeriode : grunnlag.getVurdertePerioder().getPerioder()) {
                if (vurdertOpplæringPeriode.getReisetid() != null) {
                    if (vurdertOpplæringPeriode.getReisetid().getReiseperiodeTil() != null) {
                        vurdert = vurdert.union(TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(List.of(vurdertOpplæringPeriode.getReisetid().getReiseperiodeTil()))), StandardCombinators::alwaysTrueForMatch);
                    }
                    if (vurdertOpplæringPeriode.getReisetid().getReiseperiodeHjem() != null) {
                        vurdert = vurdert.union(TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(List.of(vurdertOpplæringPeriode.getReisetid().getReiseperiodeHjem()))), StandardCombinators::alwaysTrueForMatch);
                    }
                }
            }
        }

        LocalDateTimeline<Boolean> automatisk = LocalDateTimeline.empty();

        for (OpplæringPeriodeDto periode : perioder) {
            if (periode.getReisetid() != null) {
                if (kanGodkjennesAutomatisk(periode.getReisetid().getReisetidTil())) {
                    automatisk = automatisk.union(TidslinjeUtil.tilTidslinjeKomprimert(List.of(periode.getReisetid().getReisetidTil())), StandardCombinators::alwaysTrueForMatch);
                }
                if (kanGodkjennesAutomatisk(periode.getReisetid().getReisetidHjem())) {
                    automatisk = automatisk.union(TidslinjeUtil.tilTidslinjeKomprimert(List.of(periode.getReisetid().getReisetidHjem())), StandardCombinators::alwaysTrueForMatch);
                }
            }
        }

        return vurdert.union(automatisk, StandardCombinators::alwaysTrueForMatch);
    }

    private boolean kanGodkjennesAutomatisk(Periode reisetid) {
        return reisetid != null && reisetid.getFom().equals(reisetid.getTom());
    }

    private LocalDateTimeline<Boolean> lagTidslinjeMedReisedagerSomSkalGodkjennes(List<OpplæringPeriodeDto> perioder) {
        List<LocalDate> reisedagerSomSkalGodkjennes = new ArrayList<>();

        for (OpplæringPeriodeDto periode : perioder) {
            if (periode.getReisetid() != null) {
                if (periode.getReisetid().getReisetidTil() != null) {
                    reisedagerSomSkalGodkjennes.add(periode.getReisetid().getReisetidTil().getTom());
                }
                if (periode.getReisetid().getReisetidHjem() != null) {
                    reisedagerSomSkalGodkjennes.add(periode.getReisetid().getReisetidHjem().getFom());
                }
            }
        }

        return TidslinjeUtil.tilTidslinjeKomprimert(reisedagerSomSkalGodkjennes.stream().map(localDate -> new Periode(localDate, localDate)).toList());
    }
}
