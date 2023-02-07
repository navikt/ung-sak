package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.institusjon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class InstitusjonMapper {

    private final GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;

    InstitusjonMapper(GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste) {
        this.godkjentOpplæringsinstitusjonTjeneste = godkjentOpplæringsinstitusjonTjeneste;
    }

    InstitusjonerDto mapTilDto(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<InstitusjonPeriodeDto> perioder = mapPerioder(perioderFraSøknad);
        List<InstitusjonVurderingDto> vurderinger = mapVurderinger(grunnlag, perioderFraSøknad);
        return new InstitusjonerDto(perioder, vurderinger);
    }

    private List<InstitusjonPeriodeDto> mapPerioder(Set<PerioderFraSøknad> perioderFraSøknad) {
        List<InstitusjonPeriodeDto> perioder = new ArrayList<>();

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            var kursperioder = fraSøknad.getKurs();
            var journalpostId = fraSøknad.getJournalpostId();

            for (KursPeriode kursPeriode : kursperioder) {
                perioder.add(new InstitusjonPeriodeDto(
                    new Periode(kursPeriode.getPeriode().getFomDato(), kursPeriode.getPeriode().getTomDato()),
                    kursPeriode.getInstitusjon() != null ? kursPeriode.getInstitusjon()
                        : godkjentOpplæringsinstitusjonTjeneste.hentMedUuid(kursPeriode.getInstitusjonUuid()).map(GodkjentOpplæringsinstitusjon::getNavn).orElse(null),
                    new JournalpostIdDto(journalpostId.getVerdi()))
                );
            }
        }

        return perioder;
    }

    private List<InstitusjonVurderingDto> mapVurderinger(VurdertOpplæringGrunnlag grunnlag, Set<PerioderFraSøknad> perioderFraSøknad) {
        List<InstitusjonVurderingDto> vurderinger = new ArrayList<>();

        if (grunnlag != null && grunnlag.getVurdertInstitusjonHolder() != null) {
            for (VurdertInstitusjon vurdertInstitusjon : grunnlag.getVurdertInstitusjonHolder().getVurdertInstitusjon()) {
                vurderinger.add(new InstitusjonVurderingDto(
                    new JournalpostIdDto(vurdertInstitusjon.getJournalpostId().getVerdi()),
                    finnPerioderForJournalpostId(vurdertInstitusjon.getJournalpostId(), perioderFraSøknad),
                    vurdertInstitusjon.getGodkjent() ? Resultat.GODKJENT_MANUELT : Resultat.IKKE_GODKJENT_MANUELT,
                    vurdertInstitusjon.getBegrunnelse(),
                    vurdertInstitusjon.getVurdertAv(),
                    vurdertInstitusjon.getVurdertTidspunkt())
                );
            }
        }

        for (PerioderFraSøknad fraSøknad : perioderFraSøknad) {
            var kursperioder = fraSøknad.getKurs();
            var journalpostId = fraSøknad.getJournalpostId();

            var institusjonUuid = kursperioder.stream().findAny().orElseThrow().getInstitusjonUuid();
            var perioder = kursperioder.stream().map(KursPeriode::getPeriode).toList();

            if (godkjentIRegister(institusjonUuid, perioder)) {
                vurderinger.add(new InstitusjonVurderingDto(
                    new JournalpostIdDto(journalpostId.getVerdi()),
                    perioder.stream().map(periode -> new Periode(periode.getFomDato(), periode.getTomDato())).toList(),
                    Resultat.GODKJENT_AUTOMATISK,
                    null, null, null)
                );

            } else if (vurderinger.stream().noneMatch(vurdering -> vurdering.getJournalpostId().getJournalpostId().equals(journalpostId))) {
                vurderinger.add(new InstitusjonVurderingDto(
                    new JournalpostIdDto(journalpostId.getVerdi()),
                    perioder.stream().map(periode -> new Periode(periode.getFomDato(), periode.getTomDato())).toList(),
                    Resultat.MÅ_VURDERES,
                    null, null, null)
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

    private boolean godkjentIRegister(UUID institusjonUuid, List<DatoIntervallEntitet> perioder) {
        if (institusjonUuid == null) {
            return false;
        }

        return godkjentOpplæringsinstitusjonTjeneste.hentAktivMedUuid(institusjonUuid, TidslinjeUtil.tilTidslinjeKomprimert(new TreeSet<>(perioder)))
            .isPresent();
    }
}
