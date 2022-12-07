package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertReisetid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class GjennomgåttOpplæringMapper {

    GjennomgåttOpplæringDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {

        List<OpplæringPeriodeDto> perioder = new ArrayList<>();

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {

            for (KursPeriode kursPeriode : fraSøknad.getKurs()) {
                perioder.add(new OpplæringPeriodeDto(
                    kursPeriode.getPeriode().tilPeriode(),
                    mapReisetid(kursPeriode))
                );
            }
        }

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

        return new GjennomgåttOpplæringDto(perioder, vurderinger);
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
}
