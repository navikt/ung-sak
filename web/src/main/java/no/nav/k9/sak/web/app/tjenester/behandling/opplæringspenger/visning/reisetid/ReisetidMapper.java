package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        LocalDateTimeline<ReisetidVurderingData> tidslinje = lagTidslinjeMedVurderingsdata(grunnlag, perioderFraSøknad);
        Map<Periode, List<ReisetidPeriodeVurderingDto>> periodevurderingMap = lagVurderingerOgSorterPåKursperiode(tidslinje);

        List<ReisetidVurderingDto> vurderinger = new ArrayList<>();

        for (Map.Entry<Periode, List<ReisetidPeriodeVurderingDto>> entry : periodevurderingMap.entrySet()) {
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

            vurderinger.add(new ReisetidVurderingDto(kursperiode, reisetidTil, reisetidHjem));
        }

        return vurderinger;
    }

    private LocalDateTimeline<ReisetidVurderingData> lagTidslinjeMedVurderingsdata(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<LocalDateSegment<ReisetidVurderingData>> segmenterFraGrunnlag = new ArrayList<>();
        List<LocalDateSegment<ReisetidVurderingData>> segmenterFraSøknad = new ArrayList<>();

        if (grunnlag != null && grunnlag.getVurdertReisetid() != null) {
            for (VurdertReisetid vurdertReisetid : grunnlag.getVurdertReisetid().getReisetid()) {
                segmenterFraGrunnlag.add(new LocalDateSegment<>(
                    vurdertReisetid.getPeriode().getFomDato(),
                    vurdertReisetid.getPeriode().getTomDato(),
                    new ReisetidVurderingData(
                        vurdertReisetid.getGodkjent() ? Resultat.GODKJENT : Resultat.IKKE_GODKJENT,
                        vurdertReisetid.getBegrunnelse(),
                        null)));
            }
        }

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            for (KursPeriode kursPeriode : fraSøknad.getKurs()) {

                segmenterFraSøknad.add(new LocalDateSegment<>(
                    kursPeriode.getReiseperiodeTil().getFomDato(),
                    kursPeriode.getReiseperiodeTil().getTomDato(),
                    new ReisetidVurderingData(harVarighetEnDag(
                        kursPeriode.getReiseperiodeTil().tilPeriode()) ? Resultat.GODKJENT_AUTOMATISK : Resultat.MÅ_VURDERES,
                        null,
                        kursPeriode.getPeriode().tilPeriode())));

                segmenterFraSøknad.add(new LocalDateSegment<>(
                    kursPeriode.getReiseperiodeHjem().getFomDato(),
                    kursPeriode.getReiseperiodeHjem().getTomDato(),
                    new ReisetidVurderingData(harVarighetEnDag(
                        kursPeriode.getReiseperiodeHjem().tilPeriode()) ? Resultat.GODKJENT_AUTOMATISK : Resultat.MÅ_VURDERES,
                        null,
                        kursPeriode.getPeriode().tilPeriode())));
            }
        }

        LocalDateTimeline<ReisetidVurderingData> vurdertTidslinje = new LocalDateTimeline<>(segmenterFraGrunnlag);
        LocalDateTimeline<ReisetidVurderingData> oppgittTidslinje = new LocalDateTimeline<>(segmenterFraSøknad);

        return vurdertTidslinje.combine(oppgittTidslinje, this::mergeVurdertOgOppgitt, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private LocalDateSegment<ReisetidVurderingData> mergeVurdertOgOppgitt(LocalDateInterval interval, LocalDateSegment<ReisetidVurderingData> vurdert, LocalDateSegment<ReisetidVurderingData> oppgitt) {
        Objects.requireNonNull(oppgitt, "oppgitt tidslinje må dekke vurdert tidslinje");

        if (vurdert != null) {
            return new LocalDateSegment<>(interval, new ReisetidVurderingData(vurdert.getValue().getResultat(), vurdert.getValue().getBegrunnelse(), oppgitt.getValue().getKursperiode()));
        }

        return new LocalDateSegment<>(interval, oppgitt.getValue());
    }

    private Map<Periode, List<ReisetidPeriodeVurderingDto>> lagVurderingerOgSorterPåKursperiode(LocalDateTimeline<ReisetidVurderingData> tidslinje) {
        Map<Periode, List<ReisetidPeriodeVurderingDto>> map = new HashMap<>();

        tidslinje.stream().forEach(segment -> {
            Periode kursperiode = segment.getValue().getKursperiode();
            List<ReisetidPeriodeVurderingDto> vurderingerForKursperiode = map.containsKey(kursperiode) ? new ArrayList<>(map.get(kursperiode)) : new ArrayList<>();
            vurderingerForKursperiode.add(new ReisetidPeriodeVurderingDto(segment.getFom(), segment.getTom(), segment.getValue().getResultat(), segment.getValue().getBegrunnelse()));
            map.put(kursperiode, vurderingerForKursperiode);
        });

        return map;
    }

    private boolean harVarighetEnDag(Periode periode) {
        return periode.getFom().equals(periode.getTom());
    }

    private static class ReisetidVurderingData {
        private final Resultat resultat;
        private final String begrunnelse;
        private final Periode kursperiode;

        ReisetidVurderingData(Resultat resultat, String begrunnelse, Periode kursperiode) {
            this.resultat = resultat;
            this.begrunnelse = begrunnelse;
            this.kursperiode = kursperiode;
        }

        Resultat getResultat() {
            return resultat;
        }

        String getBegrunnelse() {
            return begrunnelse;
        }

        Periode getKursperiode() {
            return kursperiode;
        }
    }
}
