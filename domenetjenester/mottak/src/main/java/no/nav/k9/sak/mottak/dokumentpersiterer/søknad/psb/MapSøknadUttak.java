package no.nav.k9.sak.mottak.dokumentpersiterer.søknad.psb;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.Ferie;
import no.nav.k9.sak.domene.uttak.repo.FeriePeriode;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning.OppgittTilsynSvar;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.TilsynsordningPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.søknad.felles.LovbestemtFerie;
import no.nav.k9.søknad.felles.Periode;
import no.nav.k9.søknad.pleiepengerbarn.Arbeid;
import no.nav.k9.søknad.pleiepengerbarn.Arbeidstaker;
import no.nav.k9.søknad.pleiepengerbarn.Frilanser;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;
import no.nav.k9.søknad.pleiepengerbarn.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.pleiepengerbarn.SøknadsperiodeInfo;
import no.nav.k9.søknad.pleiepengerbarn.Tilsynsordning;
import no.nav.k9.søknad.pleiepengerbarn.TilsynsordningOpphold;
import no.nav.k9.søknad.pleiepengerbarn.TilsynsordningSvar;

class MapSøknadUttak {
    private PleiepengerBarnSøknad søknad;

    MapSøknadUttak(PleiepengerBarnSøknad søknad) {
        this.søknad = søknad;
    }

    UttakGrunnlag getUttakGrunnlag(Long behandlingId) {
        var ferie = mapFerie(søknad.lovbestemtFerie);
        var søknadsperioder = mapSøknadsperioder(søknad.perioder);
        var oppgittUttak = mapOppgittUttak(søknad.arbeid);
        var tilsynsordning = mapOppgittTilsynsordning(søknad.tilsynsordning);
        return new UttakGrunnlag(behandlingId, oppgittUttak, søknadsperioder, ferie, tilsynsordning);
    }

    private OppgittTilsynsordning mapOppgittTilsynsordning(Tilsynsordning input) {
        if (input == null || input.opphold == null || input.opphold.isEmpty()) {
            return null;
        }
        var tilsynSvar = mapTilsynSvar(input.iTilsynsordning);
        var mappedPerioder = input.opphold.entrySet().stream()
            .map(entry -> lagTilsynsordningPeriode(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        return new OppgittTilsynsordning(mappedPerioder, tilsynSvar);
    }

    private TilsynsordningPeriode lagTilsynsordningPeriode(Periode periode, TilsynsordningOpphold opphold) {
        return new TilsynsordningPeriode(periode.fraOgMed, periode.tilOgMed, opphold == null ? Duration.ofHours(0) : opphold.lengde);
    }

    private OppgittTilsynSvar mapTilsynSvar(TilsynsordningSvar iTilsynsordning) {
        if (iTilsynsordning == null) {
            return null;
        }
        switch (iTilsynsordning) {
            case JA:
                return OppgittTilsynSvar.JA;
            case NEI:
                return OppgittTilsynSvar.NEI;
            case VET_IKKE:
                return OppgittTilsynSvar.VET_IKKE;
            default:
                throw new IllegalArgumentException("Ukjent tilsynsordning: " + iTilsynsordning);
        }
    }

    private UttakAktivitet mapOppgittUttak(Arbeid arbeid) {
        if (arbeid == null) {
            return null;
        }
        var mappedArbeid = nullableList(arbeid.arbeidstaker).stream()
            .flatMap(a -> lagUttakAktivitetPeriode(a).stream()).collect(Collectors.toList());
        var mappedFrilanser = nullableList(arbeid.frilanser).stream()
            .flatMap(f -> lagUttakAktivitetPeriode(f).stream()).collect(Collectors.toList());
        var mappedSelvstendigNæringsdrivende = nullableList(arbeid.selvstendigNæringsdrivende).stream()
            .flatMap(sn -> lagUttakAktivitetPeriode(sn).stream()).collect(Collectors.toList());

        var mappedPerioder = new ArrayList<UttakAktivitetPeriode>();
        mappedPerioder.addAll(mappedArbeid);
        mappedPerioder.addAll(mappedFrilanser);
        mappedPerioder.addAll(mappedSelvstendigNæringsdrivende);
        if (mappedPerioder.isEmpty()) {
            return null;
        }
        return new UttakAktivitet(mappedPerioder);
    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPeriode(SelvstendigNæringsdrivende input) {
        if (input == null || input.perioder == null || input.perioder.isEmpty()) {
            return Collections.emptyList();
        }
        var mappedPerioder = input.perioder.entrySet().stream()
            .map(entry -> {
                Periode k = entry.getKey();
                var v = entry.getValue();
                // TODO prosent, normal uke:
                var skalJobbeProsent = BigDecimal.valueOf(100L);
                var jobberNormaltPerUke = Duration.parse("PT37H30M");
                return new UttakAktivitetPeriode(k.fraOgMed, k.tilOgMed, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, jobberNormaltPerUke, skalJobbeProsent);
            })
            .collect(Collectors.toList());
        return mappedPerioder;

    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPeriode(Frilanser input) {
        if (input == null || input.perioder == null || input.perioder.isEmpty()) {
            return Collections.emptyList();
        }
        var mappedPerioder = input.perioder.entrySet().stream()
            .map(entry -> {
                Periode k = entry.getKey();
                var v = entry.getValue();
             // TODO prosent, normal uke:
                var skalJobbeProsent = BigDecimal.valueOf(100L);
                var jobberNormaltPerUke = Duration.parse("PT37H30M");
                return new UttakAktivitetPeriode(k.fraOgMed, k.tilOgMed, UttakArbeidType.FRILANSER, jobberNormaltPerUke, skalJobbeProsent);
            })
            .collect(Collectors.toList());
        return mappedPerioder;
    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPeriode(Arbeidstaker input) {
        if (input == null || input.perioder == null || input.perioder.isEmpty()) {
            return Collections.emptyList();
        }
        InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her, tolker om til InternArbeidsforholdRef.nullRef() ved fastsette uttak.
        var arbeidsgiver = input.organisasjonsnummer != null
            ? Arbeidsgiver.virksomhet(input.organisasjonsnummer.verdi)
            : (input.norskIdentitetsnummer != null
                ? Arbeidsgiver.fra(new AktørId(input.norskIdentitetsnummer.verdi))
                : null);

        var mappedPerioder = input.perioder.entrySet().stream()
            .map(entry -> {
                Periode k = entry.getKey();
                var v = entry.getValue();
                return new UttakAktivitetPeriode(k.fraOgMed, k.tilOgMed, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, v.jobberNormaltPerUke, v.skalJobbeProsent);
            })
            .collect(Collectors.toList());
        return mappedPerioder;
    }

    private Ferie mapFerie(LovbestemtFerie input) {
        if (input == null || input.perioder == null || input.perioder.isEmpty()) {
            return null;
        }
        var mappedPerioder = input.perioder.entrySet().stream()
            .map(entry -> new FeriePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().fraOgMed, entry.getKey().tilOgMed)))
            .collect(Collectors.toSet());

        return new Ferie(mappedPerioder);
    }

    private Søknadsperioder mapSøknadsperioder(Map<Periode, SøknadsperiodeInfo> input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        var mappedPerioder = input.entrySet().stream()
            .map(entry -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getKey().fraOgMed, entry.getKey().tilOgMed)))
            .collect(Collectors.toSet());

        return new Søknadsperioder(mappedPerioder);
    }

    private static <T> List<T> nullableList(List<T> input) {
        return input != null ? input : Collections.emptyList();
    }
}
