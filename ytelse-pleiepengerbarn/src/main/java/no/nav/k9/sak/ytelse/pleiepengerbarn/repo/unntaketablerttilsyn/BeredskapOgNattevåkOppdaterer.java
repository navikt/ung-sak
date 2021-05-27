package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BeredskapOgNattevåkOppdaterer {

    public static UnntakEtablertTilsyn oppdaterMedPerioderFraSøknad(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Long kildeBehandlingId, List<Unntaksperiode> nyeUnntak, List<Periode> unntakSomSkalSlettes) {
        nyeUnntak.forEach(unntak -> {
                if (!unntak.resultat().equals(Resultat.IKKE_VURDERT)) {
                    throw new IllegalArgumentException("Nye unntak kan bare være IKKE_VURDERT.");
                }
            }
        );
        var beskrivelser = finnUnntakEtablertTilsynBeskrivelser(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, nyeUnntak, kildeBehandlingId);
        var perioder = finnUnntakEtablertTilsynPerioder(eksisterendeUnntakEtablertTilsyn, nyeUnntak, unntakSomSkalSlettes, søkersAktørId, kildeBehandlingId);

        return new UnntakEtablertTilsyn(perioder, beskrivelser);
    }

    public static UnntakEtablertTilsyn oppdaterMedPerioderFraAksjonspunkt(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Long kildeBehandlingId, List<Unntaksperiode> bekreftetUnntaksperioder) {
        bekreftetUnntaksperioder.forEach(unntak -> {
                if (unntak.resultat().equals(Resultat.IKKE_VURDERT)) {
                    throw new IllegalArgumentException("Bekreftede unntak kan ikke være IKKE_VURDERT.");
                }
            }
        );
        var beskrivelser = finnUnntakEtablertTilsynBeskrivelser(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, bekreftetUnntaksperioder, kildeBehandlingId);
        var perioder = finnUnntakEtablertTilsynPerioder(eksisterendeUnntakEtablertTilsyn, bekreftetUnntaksperioder, List.of(), søkersAktørId, kildeBehandlingId);

        return new UnntakEtablertTilsyn(perioder, beskrivelser);
    }

    private static List<UnntakEtablertTilsynPeriode> finnUnntakEtablertTilsynPerioder(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, List<Unntaksperiode> nyeUnntak, List<Periode> unntakSomSkalSlettes, AktørId aktørId, Long kildeBehandlingId) {
        var eksisterendeSegmenter = new ArrayList<LocalDateSegment<Unntak>>();
        if (eksisterendeUnntakEtablertTilsyn != null) {
            eksisterendeUnntakEtablertTilsyn.getPerioder().forEach(periode ->
                eksisterendeSegmenter.add(new LocalDateSegment<>(periode.getPeriode().toLocalDateInterval(), new Unntak(periode.getBegrunnelse(), periode.getResultat())))
            );
        }

        var segementerSomSkalSlettes = unntakSomSkalSlettes.stream().map(periode ->
            new LocalDateSegment<>(new LocalDateInterval(periode.getFom(), periode.getTom()), new Unntak(null, null))
        ).toList();

        var segmenterSomSkalLeggesTil = nyeUnntak.stream().map(periode ->
            new LocalDateSegment<>(new LocalDateInterval(periode.fom(), periode.tom()), new Unntak(periode.begrunnelse(), periode.resultat()))
        ).toList();

        var perioderTidslinje =
            new LocalDateTimeline<>(eksisterendeSegmenter)
                .disjoint(new LocalDateTimeline<>(segementerSomSkalSlettes))
                .crossJoin(new LocalDateTimeline<>(segmenterSomSkalLeggesTil), BeredskapOgNattevåkOppdaterer::siste);

        return perioderTidslinje.toSegments().stream().map(segment ->
            new UnntakEtablertTilsynPeriode()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
                .medBegrunnelse(segment.getValue().resultat().equals(Resultat.IKKE_VURDERT) ? "" : segment.getValue().begrunnelse())
                .medAktørId(aktørId)
                .medKildeBehandlingId(kildeBehandlingId)
                .medResultat(segment.getValue().resultat())
        ).toList();
    }

    private static <T> LocalDateSegment<T> siste(LocalDateInterval dateInterval, LocalDateSegment<T> lhs, LocalDateSegment<T> rhs) {
        T lv = null;
        if (lhs != null) {
            lv = lhs.getValue();
        }
        T rv = rhs == null ? lv : rhs.getValue();
        return new LocalDateSegment<>(dateInterval, rv);
    }


    private static List<UnntakEtablertTilsynBeskrivelse> finnUnntakEtablertTilsynBeskrivelser(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, List<Unntaksperiode> nyeUnntak, Long kildeBehandlingId) {
        var beskrivelser = new ArrayList<UnntakEtablertTilsynBeskrivelse>();
        if (eksisterendeUnntakEtablertTilsyn != null) {
            beskrivelser.addAll(eksisterendeUnntakEtablertTilsyn.getBeskrivelser());
        }
        nyeUnntak.forEach(nyttUnntak -> {
                if (nyttUnntak.resultat().equals(Resultat.IKKE_VURDERT)) {
                    beskrivelser.add(new UnntakEtablertTilsynBeskrivelse(
                        DatoIntervallEntitet.fraOgMedTilOgMed(nyttUnntak.fom(), nyttUnntak.tom()),
                        mottattDato,
                        nyttUnntak.begrunnelse(),
                        søkersAktørId,
                        kildeBehandlingId));
                }
            }
        );
        return beskrivelser;
    }

}

