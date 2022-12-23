package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class ReisetidMapper {

    ReisetidDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<ReisetidPeriodeDto> perioder = mapPerioder(perioderFraSøknad);
        List<ReisetidVurderingDto> vurderinger = mapVurderinger(grunnlag, perioderFraSøknad);
        return new ReisetidDto(perioder, vurderinger);
    }

    private List<ReisetidPeriodeDto> mapPerioder(Set<PerioderFraSøknad> perioderFraSøknad) {
        List<ReisetidPeriodeDto> perioder = new ArrayList<>();

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            for (KursPeriode kursPeriode : fraSøknad.getKurs()) {
                perioder.add(new ReisetidPeriodeDto(
                    kursPeriode.getPeriode().tilPeriode(),
                    kursPeriode.getReiseperiodeTil().tilPeriode(),
                    kursPeriode.getReiseperiodeHjem().tilPeriode()));
            }
        }

        return perioder;
    }

    private List<ReisetidVurderingDto> mapVurderinger(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<ReisetidPeriodeVurderingDto> periodevurderinger = new ArrayList<>();

        List<LocalDateSegment<Boolean>> segmenterVurdert = new ArrayList<>();
        if (grunnlag != null && grunnlag.getVurdertReisetid() != null) {
            for (VurdertReisetid vurdertReisetid : grunnlag.getVurdertReisetid().getReisetid()) {
                periodevurderinger.add(new ReisetidPeriodeVurderingDto(vurdertReisetid.getPeriode().tilPeriode(),
                    vurdertReisetid.getGodkjent() ? Resultat.GODKJENT : Resultat.IKKE_GODKJENT,
                    vurdertReisetid.getBegrunnelse()));

                segmenterVurdert.add(new LocalDateSegment<>(
                    vurdertReisetid.getPeriode().getFomDato(),
                    vurdertReisetid.getPeriode().getTomDato(),
                    true));
            }
        }

        List<LocalDateSegment<Resultat>> segmenterOppgitt = new ArrayList<>();
        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            for (KursPeriode kursPeriode : fraSøknad.getKurs()) {
                segmenterOppgitt.add(new LocalDateSegment<>(
                    kursPeriode.getReiseperiodeTil().getFomDato(),
                    kursPeriode.getReiseperiodeTil().getTomDato(),
                    harVarighetEnDag(kursPeriode.getReiseperiodeTil().tilPeriode()) ? Resultat.GODKJENT_AUTOMATISK : Resultat.MÅ_VURDERES));
                segmenterOppgitt.add(new LocalDateSegment<>(
                    kursPeriode.getReiseperiodeHjem().getFomDato(),
                    kursPeriode.getReiseperiodeHjem().getTomDato(),
                    harVarighetEnDag(kursPeriode.getReiseperiodeHjem().tilPeriode()) ? Resultat.GODKJENT_AUTOMATISK : Resultat.MÅ_VURDERES));
            }
        }

        LocalDateTimeline<Boolean> vurdertTidslinje = new LocalDateTimeline<>(segmenterVurdert);
        LocalDateTimeline<Resultat> oppgittTidslinje = new LocalDateTimeline<>(segmenterOppgitt);

        oppgittTidslinje.disjoint(vurdertTidslinje).stream().forEach(segment ->
            periodevurderinger.add(new ReisetidPeriodeVurderingDto(segment.getFom(), segment.getTom(), segment.getValue(), null)));

        LocalDateTimeline<Periode> tidslinjeMedKursperiode = lagTidslinjeForReisetidMedTilhørendeKursperiode(perioderFraSøknad);

        Map<Periode, List<ReisetidPeriodeVurderingDto>> map = new HashMap<>();

        for (ReisetidPeriodeVurderingDto periodevurdering : periodevurderinger) {
            Periode kursperiode = tidslinjeMedKursperiode.getSegment(new LocalDateInterval(periodevurdering.getPeriode().getFom(), periodevurdering.getPeriode().getFom())).getValue();
            List<ReisetidPeriodeVurderingDto> vurderingerForKursperiode = map.containsKey(kursperiode) ? new ArrayList<>(map.get(kursperiode)) : new ArrayList<>();
            vurderingerForKursperiode.add(periodevurdering);
            map.put(kursperiode, vurderingerForKursperiode);
        }

        List<ReisetidVurderingDto> vurderinger = new ArrayList<>();

        for (Map.Entry<Periode, List<ReisetidPeriodeVurderingDto>> entry : map.entrySet()) {
            Periode kursperiode = entry.getKey();
            List<ReisetidPeriodeVurderingDto> reisetidTil = new ArrayList<>();
            List<ReisetidPeriodeVurderingDto> reisetidHjem = new ArrayList<>();
            for (ReisetidPeriodeVurderingDto periodevurdering : entry.getValue()) {
                if (periodevurdering.getPeriode().getFom().isBefore(kursperiode.getFom())) {
                    reisetidTil.add(periodevurdering);
                } else {
                    reisetidHjem.add(periodevurdering);
                }
            }
            vurderinger.add(new ReisetidVurderingDto(kursperiode, reisetidTil, reisetidHjem, null));
        }

        return vurderinger;
    }

    private LocalDateTimeline<Periode> lagTidslinjeForReisetidMedTilhørendeKursperiode(Set<PerioderFraSøknad> perioderFraSøknad) {
        List<LocalDateSegment<Periode>> segmenter = new ArrayList<>();
        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            for (KursPeriode kursPeriode : fraSøknad.getKurs()) {
                segmenter.add(new LocalDateSegment<>(
                    kursPeriode.getReiseperiodeTil().getFomDato(),
                    kursPeriode.getReiseperiodeTil().getTomDato(),
                    kursPeriode.getPeriode().tilPeriode()));
                segmenter.add(new LocalDateSegment<>(
                    kursPeriode.getReiseperiodeHjem().getFomDato(),
                    kursPeriode.getReiseperiodeHjem().getTomDato(),
                    kursPeriode.getPeriode().tilPeriode()));
            }
        }
        return new LocalDateTimeline<>(segmenter);
    }

    private boolean harVarighetEnDag(Periode periode) {
        return periode.getFom().equals(periode.getTom());
    }
}
