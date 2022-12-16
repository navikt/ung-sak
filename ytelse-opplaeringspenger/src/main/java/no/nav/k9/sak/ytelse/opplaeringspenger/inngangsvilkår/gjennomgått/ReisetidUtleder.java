package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class ReisetidUtleder {

    static LocalDateTimeline<Boolean> finnOppgittReisetid(Set<PerioderFraSøknad> perioderFraSøknad) {
        Objects.requireNonNull(perioderFraSøknad);

        NavigableSet<DatoIntervallEntitet> reisetid = new TreeSet<>();
        for (PerioderFraSøknad perioder : perioderFraSøknad) {
            for (KursPeriode kursPeriode : perioder.getKurs()) {
                if (kursPeriode.getReiseperiodeTil() != null) {
                    reisetid.add(kursPeriode.getReiseperiodeTil());
                }
                if (kursPeriode.getReiseperiodeHjem() != null) {
                    reisetid.add(kursPeriode.getReiseperiodeHjem());
                }
            }
        }
        return TidslinjeUtil.tilTidslinjeKomprimert(reisetid);
    }

    static LocalDateTimeline<OpplæringGodkjenningStatus> utledVurdertReisetid(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {
        Objects.requireNonNull(perioderFraSøknad);

        List<LocalDateSegment<OpplæringGodkjenningStatus>> segmenter = new ArrayList<>();
        for (PerioderFraSøknad perioder : perioderFraSøknad) {
            for (KursPeriode kursPeriode : perioder.getKurs()) {
                if (kanGodkjennesAutomatisk(kursPeriode.getReiseperiodeTil())) {
                    segmenter.add(new LocalDateSegment<>(
                        kursPeriode.getReiseperiodeTil().getFomDato(),
                        kursPeriode.getReiseperiodeTil().getTomDato(),
                        OpplæringGodkjenningStatus.GODKJENT));
                }
                if (kanGodkjennesAutomatisk(kursPeriode.getReiseperiodeHjem())) {
                    segmenter.add(new LocalDateSegment<>(
                        kursPeriode.getReiseperiodeHjem().getFomDato(),
                        kursPeriode.getReiseperiodeHjem().getTomDato(),
                        OpplæringGodkjenningStatus.GODKJENT));
                }
            }
        }
        if (vurdertOpplæringGrunnlag != null && vurdertOpplæringGrunnlag.getVurdertReisetid() != null) {
            List<VurdertReisetid> vurdertReisetid = vurdertOpplæringGrunnlag.getVurdertReisetid().getReisetid().stream().toList();
            for (VurdertReisetid reisetid : vurdertReisetid) {
                reisetid.getReiseperioder()
                    .forEach(reiseperiode -> segmenter.add(new LocalDateSegment<>(
                        reiseperiode.getPeriode().getFomDato(),
                        reiseperiode.getPeriode().getTomDato(),
                        reiseperiode.getGodkjent() ? OpplæringGodkjenningStatus.GODKJENT : OpplæringGodkjenningStatus.IKKE_GODKJENT_REISETID)));
            }
        }
        return new LocalDateTimeline<>(segmenter);
    }

    private static boolean kanGodkjennesAutomatisk(DatoIntervallEntitet reisetid) {
        return reisetid != null && (reisetid.getFomDato().equals(reisetid.getTomDato()));
    }
}
