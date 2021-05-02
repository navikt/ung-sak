package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.untaketablerttilsyn;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.sykdom.Resultat;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap;
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BeredskapOgNattevåkOversetter {

    public static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Beredskap beredskap) {
        var nyeUnntakBeredskap = new ArrayList<Unntaksperiode>();
        beredskap.getPerioder().forEach( (key,value) ->
            nyeUnntakBeredskap.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), value.getTilleggsinformasjon()))
        );
        var unntakSomSkalSlettes = new ArrayList<Unntaksperiode>();
        beredskap.getPerioderSomSkalSlettes().forEach( (key,value) ->
            nyeUnntakBeredskap.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), null))
        );
        return tilUnntakEtablertTilsynForPleietrengende(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, nyeUnntakBeredskap, unntakSomSkalSlettes);
    }

    public static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, Nattevåk nattevåk) {
        var nyeUnntakNattevåk = new ArrayList<Unntaksperiode>();
        nattevåk.getPerioder().forEach( (key,value) ->
            nyeUnntakNattevåk.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), value.getTilleggsinformasjon()))
        );
        var unntakSomSkalSlettes = new ArrayList<Unntaksperiode>();
        nattevåk.getPerioderSomSkalSlettes().forEach( (key,value) ->
            nyeUnntakNattevåk.add(new Unntaksperiode(key.getFraOgMed(), key.getTilOgMed(), null))
        );
        return tilUnntakEtablertTilsynForPleietrengende(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, nyeUnntakNattevåk, unntakSomSkalSlettes);
    }


    private static UnntakEtablertTilsyn tilUnntakEtablertTilsynForPleietrengende(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, List<Unntaksperiode> nyeUnntak, List<Unntaksperiode> unntakSomSkalSlettes) {
        var beskrivelser = finnUnntakEtablertTilsynBeskrivelser(eksisterendeUnntakEtablertTilsyn, mottattDato, søkersAktørId, nyeUnntak);
        var perioder = finnUnntakEtablertTilsynPerioder(eksisterendeUnntakEtablertTilsyn, nyeUnntak, unntakSomSkalSlettes);

        var unntakEtablertTilsynForBeredskap = new UnntakEtablertTilsyn();
        unntakEtablertTilsynForBeredskap.setBeskrivelser(beskrivelser);
        unntakEtablertTilsynForBeredskap.setPerioder(perioder);

        return unntakEtablertTilsynForBeredskap;
    }

    private static List<UnntakEtablertTilsynPeriode> finnUnntakEtablertTilsynPerioder(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, List<Unntaksperiode> nyeUnntak, List<Unntaksperiode> unntakSomSkalSlettes) {
        var eksisterendeSegmenter = eksisterendeUnntakEtablertTilsyn.getPerioder().stream().map(periode ->
            new LocalDateSegment<>(periode.getPeriode().toLocalDateInterval(), new Unntak(periode.getBegrunnelse(), periode.getResultat()))
        ).toList();

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

        return perioderTidslinje.toSegments().stream().map(segment -> new UnntakEtablertTilsynPeriode()).toList();
    }

    @NotNull
    private static ArrayList<UnntakEtablertTilsynBeskrivelse> finnUnntakEtablertTilsynBeskrivelser(UnntakEtablertTilsyn eksisterendeUnntakEtablertTilsyn, LocalDate mottattDato, AktørId søkersAktørId, List<Unntaksperiode> nyeUnntak) {
        var beskrivelser = new ArrayList<>(eksisterendeUnntakEtablertTilsyn.getBeskrivelser());
        nyeUnntak.forEach(nyttUnntak ->
            beskrivelser.add(new UnntakEtablertTilsynBeskrivelse(
                DatoIntervallEntitet.fraOgMedTilOgMed(nyttUnntak.fom(), nyttUnntak.tom()),
                mottattDato,
                nyttUnntak.tilleggsinformasjon(),
                søkersAktørId))
        );
        return beskrivelser;
    }

}

record Unntaksperiode(LocalDate fom, LocalDate tom, String tilleggsinformasjon) {}

record Unntak(String begrunnelse, Resultat resultat) {}
