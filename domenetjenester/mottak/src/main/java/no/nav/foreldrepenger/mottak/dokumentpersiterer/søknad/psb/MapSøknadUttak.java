package no.nav.foreldrepenger.mottak.dokumentpersiterer.søknad.psb;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
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

    private OppgittTilsynsordning mapOppgittTilsynsordning(Tilsynsordning tilsynsordning) {
        var tilsynSvar = mapTilsynSvar(tilsynsordning.iTilsynsordning);
        var mappedPerioder = tilsynsordning.opphold.entrySet().stream()
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
        var mappedArbeid = arbeid.arbeidstaker.stream()
            .flatMap(a -> lagUttakAktivitetPeriode(a).stream()).collect(Collectors.toList());
        var mappedFrilanser = arbeid.frilanser.stream()
            .flatMap(f -> lagUttakAktivitetPeriode(f).stream()).collect(Collectors.toList());
        var mappedSelvstendigNæringsdrivende = arbeid.selvstendigNæringsdrivende.stream()
            .flatMap(sn -> lagUttakAktivitetPeriode(sn).stream()).collect(Collectors.toList());

        var mappedPerioder = new ArrayList<UttakAktivitetPeriode>();
        mappedPerioder.addAll(mappedArbeid);
        mappedPerioder.addAll(mappedFrilanser);
        mappedPerioder.addAll(mappedSelvstendigNæringsdrivende);
        return new UttakAktivitet(mappedPerioder);
    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPeriode(SelvstendigNæringsdrivende selvstendigNæringsdrivende) {
        var mappedPerioder = selvstendigNæringsdrivende.perioder.entrySet().stream()
            .map(input -> new UttakAktivitetPeriode(input.getKey().fraOgMed, input.getKey().tilOgMed, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE))
            .collect(Collectors.toList());
        return mappedPerioder;
    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPeriode(Frilanser frilanser) {
        var mappedPerioder = frilanser.perioder.entrySet().stream()
            .map(input -> new UttakAktivitetPeriode(input.getKey().fraOgMed, input.getKey().tilOgMed, UttakArbeidType.FRILANSER))
            .collect(Collectors.toList());
        return mappedPerioder;
    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPeriode(Arbeidstaker arbeidstaker) {
        InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her, tolker om til InternArbeidsforholdRef.nullRef() ved fastsettelse
        var arbeidsgiver = arbeidstaker.organisasjonsnummer != null
            ? Arbeidsgiver.virksomhet(arbeidstaker.organisasjonsnummer.verdi)
            : (arbeidstaker.norskIdentitetsnummer != null
                ? Arbeidsgiver.fra(new AktørId(arbeidstaker.norskIdentitetsnummer.verdi))
                : null);

        var mappedPerioder = arbeidstaker.perioder.entrySet().stream()
            .map(input -> new UttakAktivitetPeriode(input.getKey().fraOgMed, input.getKey().tilOgMed, UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef))
            .collect(Collectors.toList());
        return mappedPerioder;
    }

    private Ferie mapFerie(LovbestemtFerie oppgittFerie) {
        var mappedPerioder = oppgittFerie.perioder.entrySet().stream()
            .map(input -> new FeriePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(input.getKey().fraOgMed, input.getKey().tilOgMed)))
            .collect(Collectors.toSet());

        return new Ferie(mappedPerioder);
    }

    private Søknadsperioder mapSøknadsperioder(Map<Periode, SøknadsperiodeInfo> perioder) {
        var mappedPerioder = perioder.entrySet().stream()
            .map(input -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(input.getKey().fraOgMed, input.getKey().tilOgMed)))
            .collect(Collectors.toSet());

        return new Søknadsperioder(mappedPerioder);
    }
}
