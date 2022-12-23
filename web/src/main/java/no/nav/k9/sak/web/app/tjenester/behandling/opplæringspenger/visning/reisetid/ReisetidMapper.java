package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class ReisetidMapper {

    ReisetidDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<ReisetidPeriodeDto> perioder = mapPerioder(perioderFraSøknad);
        List<ReisetidVurderingDto> vurderinger = mapVurderinger(grunnlag, perioder);
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

    private List<ReisetidVurderingDto> mapVurderinger(VurdertOpplæringGrunnlag grunnlag, List<ReisetidPeriodeDto> perioder) {
        List<ReisetidVurderingDto> vurderinger = new ArrayList<>();

        if (grunnlag != null && grunnlag.getVurdertReisetid() != null) {
            for (VurdertReisetid vurdertReisetid : grunnlag.getVurdertReisetid().getReisetid()) {
                Periode opplæringPeriode = finnOpplæringsperiodeFraReisetid(vurdertReisetid.getPeriode(), perioder);

                List<ReisetidPeriodeVurderingDto> reisetidTil = new ArrayList<>();
                List<ReisetidPeriodeVurderingDto> reisetidHjem = new ArrayList<>();

                if (vurdertReisetid.getPeriode().getFomDato().isBefore(opplæringPeriode.getFom())) {
                    reisetidTil.add(new ReisetidPeriodeVurderingDto(
                        vurdertReisetid.getPeriode().tilPeriode(),
                        vurdertReisetid.getGodkjent() ? Resultat.GODKJENT : Resultat.IKKE_GODKJENT));
                } else {
                    reisetidHjem.add(new ReisetidPeriodeVurderingDto(
                        vurdertReisetid.getPeriode().tilPeriode(),
                        vurdertReisetid.getGodkjent() ? Resultat.GODKJENT : Resultat.IKKE_GODKJENT));
                }

                LocalDateTimeline<Boolean> oppgittTidslinjeTil = TidslinjeUtil.tilTidslinjeKomprimert(List.of(perioder.stream()
                    .filter(reisetidPeriode -> reisetidPeriode.getOpplæringPeriode().equals(opplæringPeriode)).findFirst().orElseThrow().getReisetidTil()));
                if (harVarighetEnDag(oppgittTidslinjeTil)) {
                    reisetidTil.add(new ReisetidPeriodeVurderingDto(new Periode(oppgittTidslinjeTil.getMinLocalDate(), oppgittTidslinjeTil.getMaxLocalDate()), Resultat.GODKJENT_AUTOMATISK));
                } else {
                    LocalDateTimeline<Boolean> vurdertTidslinjeTil = TidslinjeUtil.tilTidslinjeKomprimert(reisetidTil.stream()
                        .map(ReisetidPeriodeVurderingDto::getPeriode).toList());
                    List<Periode> ikkevurdertePerioderTil = TidslinjeUtil.tilPerioder(oppgittTidslinjeTil.disjoint(vurdertTidslinjeTil));
                    reisetidTil.addAll(ikkevurdertePerioderTil.stream()
                        .map(ikkeVurdertPeriode -> new ReisetidPeriodeVurderingDto(ikkeVurdertPeriode, Resultat.MÅ_VURDERES)).toList());
                }

                LocalDateTimeline<Boolean> oppgittTidslinjeHjem = TidslinjeUtil.tilTidslinjeKomprimert(List.of(perioder.stream()
                    .filter(reisetidPeriode -> reisetidPeriode.getOpplæringPeriode().equals(opplæringPeriode)).findFirst().orElseThrow().getReisetidHjem()));
                if (harVarighetEnDag(oppgittTidslinjeHjem)) {
                    reisetidHjem.add(new ReisetidPeriodeVurderingDto(new Periode(oppgittTidslinjeHjem.getMinLocalDate(), oppgittTidslinjeHjem.getMaxLocalDate()), Resultat.GODKJENT_AUTOMATISK));
                } else {
                    LocalDateTimeline<Boolean> vurdertTidslinjeHjem = TidslinjeUtil.tilTidslinjeKomprimert(reisetidHjem.stream()
                        .map(ReisetidPeriodeVurderingDto::getPeriode).toList());
                    List<Periode> ikkevurdertePerioderHjem = TidslinjeUtil.tilPerioder(oppgittTidslinjeHjem.disjoint(vurdertTidslinjeHjem));
                    reisetidHjem.addAll(ikkevurdertePerioderHjem.stream()
                        .map(ikkeVurdertPeriode -> new ReisetidPeriodeVurderingDto(ikkeVurdertPeriode, Resultat.MÅ_VURDERES)).toList());
                }

                vurderinger.add(new ReisetidVurderingDto(opplæringPeriode, reisetidTil, reisetidHjem, vurdertReisetid.getBegrunnelse()));
            }
        }

        for (ReisetidPeriodeDto periode : perioder) {
            if (vurderinger.stream().noneMatch(vurdering -> vurdering.getOpplæringPeriode().equals(periode.getOpplæringPeriode()))) {
                vurderinger.add(new ReisetidVurderingDto(periode.getOpplæringPeriode(),
                    List.of(new ReisetidPeriodeVurderingDto(
                        periode.getReisetidTil(),
                        harVarighetEnDag(periode.getReisetidTil()) ? Resultat.GODKJENT_AUTOMATISK : Resultat.MÅ_VURDERES)),
                    List.of(new ReisetidPeriodeVurderingDto(
                        periode.getReisetidHjem(),
                        harVarighetEnDag(periode.getReisetidHjem()) ? Resultat.GODKJENT_AUTOMATISK : Resultat.MÅ_VURDERES)),
                    null));
            }
        }

        return vurderinger;
    }

    private Periode finnOpplæringsperiodeFraReisetid(DatoIntervallEntitet reisetid, List<ReisetidPeriodeDto> perioder) {
        List<LocalDateSegment<Periode>> segmenter = new ArrayList<>();
        for (ReisetidPeriodeDto periodeDto : perioder) {
            segmenter.add(new LocalDateSegment<>(
                periodeDto.getReisetidTil().getFom(),
                periodeDto.getReisetidTil().getTom(),
                periodeDto.getOpplæringPeriode()));
            segmenter.add(new LocalDateSegment<>(
                periodeDto.getReisetidHjem().getFom(),
                periodeDto.getReisetidHjem().getTom(),
                periodeDto.getOpplæringPeriode()));
        }
        LocalDateTimeline<Periode> tidslinje = new LocalDateTimeline<>(segmenter);
        return tidslinje.getSegment(new LocalDateInterval(reisetid.getFomDato(), reisetid.getTomDato())).getValue();
    }

    private boolean harVarighetEnDag(LocalDateTimeline<Boolean> tidslinje) {
        return tidslinje.getMinLocalDate().equals(tidslinje.getMaxLocalDate());
    }

    private boolean harVarighetEnDag(Periode periode) {
        return periode.getFom().equals(periode.getTom());
    }
}
