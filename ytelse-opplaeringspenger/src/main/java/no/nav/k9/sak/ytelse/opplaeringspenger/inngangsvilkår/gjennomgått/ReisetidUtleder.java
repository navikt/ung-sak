package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
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

    static LocalDateTimeline<Boolean> utledGodkjentReisetid(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {
        Objects.requireNonNull(perioderFraSøknad);

        NavigableSet<DatoIntervallEntitet> godkjentReisetid = new TreeSet<>();
        for (PerioderFraSøknad perioder : perioderFraSøknad) {
            for (KursPeriode kursPeriode : perioder.getKurs()) {
                if (kanGodkjennesAutomatisk(kursPeriode.getReiseperiodeTil())) {
                    godkjentReisetid.add(kursPeriode.getReiseperiodeTil());
                }
                if (kanGodkjennesAutomatisk(kursPeriode.getReiseperiodeHjem())) {
                    godkjentReisetid.add(kursPeriode.getReiseperiodeHjem());
                }
            }
        }
        if (vurdertOpplæringGrunnlag != null && vurdertOpplæringGrunnlag.getVurdertePerioder() != null) {
            List<VurdertReisetid> vurdertReisetid = vurdertOpplæringGrunnlag.getVurdertePerioder().getPerioder().stream().map(VurdertOpplæringPeriode::getReisetid).filter(Objects::nonNull).toList();
            for (VurdertReisetid reisetid : vurdertReisetid) {
                if (reisetid.getReiseperiodeTil() != null) {
                    godkjentReisetid.add(reisetid.getReiseperiodeTil());
                }
                if (reisetid.getReiseperiodeHjem() != null) {
                    godkjentReisetid.add(reisetid.getReiseperiodeHjem());
                }
            }
        }
        return TidslinjeUtil.tilTidslinjeKomprimert(godkjentReisetid);
    }

    private static boolean kanGodkjennesAutomatisk(DatoIntervallEntitet reisetid) {
        return reisetid != null && (reisetid.getFomDato().equals(reisetid.getTomDato()));
    }
}
