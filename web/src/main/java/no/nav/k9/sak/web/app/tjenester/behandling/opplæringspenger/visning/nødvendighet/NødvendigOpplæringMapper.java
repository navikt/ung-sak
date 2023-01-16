package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.nødvendighet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class NødvendigOpplæringMapper {

    NødvendigOpplæringDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<NødvendighetPeriodeDto> beskrivelser = mapPerioder(perioderFraSøknad);
        List<NødvendighetVurderingDto> vurderinger = mapVurderinger(grunnlag, perioderFraSøknad);
        return new NødvendigOpplæringDto(beskrivelser, vurderinger);
    }

    private List<NødvendighetPeriodeDto> mapPerioder(Set<PerioderFraSøknad> perioderFraSøknad) {
        List<NødvendighetPeriodeDto> perioder = new ArrayList<>();

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            var kursperioder = fraSøknad.getKurs();
            var journalpostId = fraSøknad.getJournalpostId();

            for (KursPeriode kursPeriode : kursperioder) {
                perioder.add(new NødvendighetPeriodeDto(
                    new Periode(kursPeriode.getPeriode().getFomDato(), kursPeriode.getPeriode().getTomDato()),
                    new JournalpostIdDto(journalpostId.getVerdi()))
                );
            }
        }

        return perioder;
    }

    private List<NødvendighetVurderingDto> mapVurderinger(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<NødvendighetVurderingDto> vurderinger = new ArrayList<>();

        if (grunnlag != null && grunnlag.getVurdertOpplæringHolder() != null) {
            for (VurdertOpplæring vurdertOpplæring : grunnlag.getVurdertOpplæringHolder().getVurdertOpplæring()) {
                vurderinger.add(new NødvendighetVurderingDto(
                    new JournalpostIdDto(vurdertOpplæring.getJournalpostId().getVerdi()),
                    finnPerioderForJournalpostId(vurdertOpplæring.getJournalpostId(), perioderFraSøknad),
                    vurdertOpplæring.getNødvendigOpplæring() ? Resultat.GODKJENT : Resultat.IKKE_GODKJENT,
                    vurdertOpplæring.getBegrunnelse())
                );
            }
        }

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            var perioder = fraSøknad.getKurs().stream().map(KursPeriode::getPeriode).toList();
            var journalpostId = fraSøknad.getJournalpostId();

            if (vurderinger.stream().noneMatch(vurdering -> vurdering.getJournalpostId().getJournalpostId().equals(journalpostId))) {
                vurderinger.add(new NødvendighetVurderingDto(
                    new JournalpostIdDto(journalpostId.getVerdi()),
                    perioder.stream().map(periode -> new Periode(periode.getFomDato(), periode.getTomDato())).toList(),
                    Resultat.MÅ_VURDERES,
                    null)
                );
            }
        }

        return vurderinger;
    }

    private List<Periode> finnPerioderForJournalpostId(JournalpostId journalpostId, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<KursPeriode> perioder = new ArrayList<>();
        for (PerioderFraSøknad periode : perioderFraSøknad) {
            if (periode.getJournalpostId().equals(journalpostId)) {
                perioder.addAll(periode.getKurs());
            }
        }
        return perioder.stream().map(kursPeriode -> new Periode(kursPeriode.getPeriode().getFomDato(), kursPeriode.getPeriode().getTomDato())).toList();
    }
}
