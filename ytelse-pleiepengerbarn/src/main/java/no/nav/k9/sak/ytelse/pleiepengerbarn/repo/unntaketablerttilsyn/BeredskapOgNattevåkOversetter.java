package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BeredskapOgNattevåkOversetter {


    public static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Long kildeBehandlingId, String vurderingstekst, List<Unntaksperiode> nyeUnntak, List<Unntaksperiode> unntakSomSkalSlettes) {
        var beskrivelser = finnUnntakEtablertTilsynBeskrivelser(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, nyeUnntak, kildeBehandlingId);
        var perioder = finnUnntakEtablertTilsynPerioder(eksisterendeUnntakEtablertTilsyn, nyeUnntak, unntakSomSkalSlettes, søkersAktørId, kildeBehandlingId, vurderingstekst);

        var unntakEtablertTilsynForBeredskap = new UnntakEtablertTilsyn(perioder, beskrivelser);

        return unntakEtablertTilsynForBeredskap;
    }

    private static List<UnntakEtablertTilsynPeriode> finnUnntakEtablertTilsynPerioder(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, List<Unntaksperiode> nyeUnntak, List<Unntaksperiode> unntakSomSkalSlettes, AktørId aktørId, Long kildeBehandlingId, String vurderingstekst) {
        var eksisterendeSegmenter = new ArrayList<LocalDateSegment<Unntak>>();
        if (eksisterendeUnntakEtablertTilsyn != null) {
            eksisterendeUnntakEtablertTilsyn.getPerioder().forEach(periode ->
                eksisterendeSegmenter.add(new LocalDateSegment<>(periode.getPeriode().toLocalDateInterval(), new Unntak(periode.getBegrunnelse(), periode.getResultat())))
            );
        }

        var segementerSomSkalSlettes = unntakSomSkalSlettes.stream().map(periode ->
            new LocalDateSegment<>(new LocalDateInterval(periode.fom(), periode.tom()), new Unntak("", Resultat.IKKE_OPPFYLT))
        ).toList();

        var segmenterSomSkalLeggesTil = nyeUnntak.stream().map(periode ->
            new LocalDateSegment<>(new LocalDateInterval(periode.fom(), periode.tom()), new Unntak("", Resultat.IKKE_VURDERT))
        ).toList();

        var perioderTidslinje =
            new LocalDateTimeline<>(eksisterendeSegmenter)
                .disjoint(new LocalDateTimeline<>(segementerSomSkalSlettes))
                .crossJoin(new LocalDateTimeline<>(segmenterSomSkalLeggesTil));

        return perioderTidslinje.toSegments().stream().map(segment ->
            new UnntakEtablertTilsynPeriode()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
                .medBegrunnelse(vurderingstekst)
                .medAktørId(aktørId)
                .medKildeBehandlingId(kildeBehandlingId)
                .medResultat(Resultat.IKKE_VURDERT)
        ).toList();
    }

    private static List<UnntakEtablertTilsynBeskrivelse> finnUnntakEtablertTilsynBeskrivelser(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, List<Unntaksperiode> nyeUnntak, Long kildeBehandlingId) {
        var beskrivelser = new ArrayList<UnntakEtablertTilsynBeskrivelse>();
        if (eksisterendeUnntakEtablertTilsyn != null) {
            beskrivelser.addAll(eksisterendeUnntakEtablertTilsyn.getBeskrivelser());
        }
        nyeUnntak.forEach(nyttUnntak ->
            beskrivelser.add(new UnntakEtablertTilsynBeskrivelse(
                eksisterendeUnntakEtablertTilsyn,
                DatoIntervallEntitet.fraOgMedTilOgMed(nyttUnntak.fom(), nyttUnntak.tom()),
                mottattDato,
                nyttUnntak.tilleggsinformasjon(),
                søkersAktørId,
                kildeBehandlingId))
        );
        return beskrivelser;
    }

}

