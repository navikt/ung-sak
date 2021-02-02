package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
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
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.LovbestemtFerie;
import no.nav.k9.søknad.felles.aktivitet.Arbeidstaker;
import no.nav.k9.søknad.felles.aktivitet.Frilanser;
import no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;
import no.nav.k9.søknad.ytelse.psb.v1.Uttak;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo;
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo;
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning;

class MapSøknadUttak {
    @SuppressWarnings("unused")
    private Søknad søknad;
    private PleiepengerSyktBarn ytelse;

    MapSøknadUttak(Søknad søknad) {
        this.søknad = søknad;
        this.ytelse = søknad.getYtelse();
    }

    UttakGrunnlag getUttakGrunnlag(Long behandlingId) {
        var ferie = mapFerie(ytelse.getLovbestemtFerie());
        var søknadsperioder = mapSøknadsperioder(ytelse.getUttak().getPerioder().keySet());
        var oppgittArbeidstid = mapOppgittArbeidstid(ytelse.getArbeidstid()); // TODO FUNKER IKKE
        var tilsynsordning = mapOppgittTilsynsordning(ytelse.getTilsynsordning());
        return new UttakGrunnlag(behandlingId, oppgittArbeidstid, søknadsperioder, ferie, tilsynsordning);
    }

    private OppgittTilsynsordning mapOppgittTilsynsordning(Tilsynsordning input) {
        if (input == null || input.getPerioder() == null || input.getPerioder().isEmpty()) {
            return null;
        }

        var tilsynSvar = OppgittTilsynSvar.JA; // TODO hvordan mappe denne
        var mappedPerioder = input.getPerioder().entrySet().stream()
            .map(entry -> lagTilsynsordningPeriode(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        return new OppgittTilsynsordning(mappedPerioder, tilsynSvar);
    }

    private TilsynsordningPeriode lagTilsynsordningPeriode(Periode periode, TilsynPeriodeInfo tilsynPeriodeInfo) {
        return new TilsynsordningPeriode(periode.getFraOgMed(), periode.getTilOgMed(), tilsynPeriodeInfo == null ? Duration.ofHours(0) : tilsynPeriodeInfo.getEtablertTilsynTimerPerDag());
    }

    private UttakAktivitet mapOppgittArbeidstid(Arbeidstid arbeidstid) {
        if (arbeidstid == null) {
            return null;
        }
        var mappedUttak = nullableList(arbeidstid.getArbeidstakerList()).stream()
            .flatMap(a -> lagUttakAktivitetPeriode(a).stream()).collect(Collectors.toList());
//        var mappedFrilanser = lagUttakAktivitetPeriode(arbeid.getFrilanser());
//        var mappedSelvstendigNæringsdrivende = nullableList(arbeid.getSelvstendigNæringsdrivende()).stream()
//            .flatMap(sn -> lagUttakAktivitetPeriode(sn).stream()).collect(Collectors.toList());

        var mappedPerioder = new ArrayList<UttakAktivitetPeriode>();
        mappedPerioder.addAll(mappedUttak);
//        mappedPerioder.addAll(mappedFrilanser == null ? List.of() : List.of(mappedFrilanser));
//        mappedPerioder.addAll(mappedSelvstendigNæringsdrivende);
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
                // TODO prosent, normal uke:
                var skalJobbeProsent = BigDecimal.valueOf(100L);
                var jobberNormaltPerUke = Duration.parse("PT37H30M");
                return new UttakAktivitetPeriode(k.getFraOgMed(), k.getTilOgMed(), UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, jobberNormaltPerUke, skalJobbeProsent);
            })
            .collect(Collectors.toList());
        return mappedPerioder;

    }

    private UttakAktivitetPeriode lagUttakAktivitetPeriode(Frilanser input) {
        if (input == null || input.startdato == null) {
            return null;
        }
        // TODO: trengs noen perioder for frilanser?
        return new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, DatoIntervallEntitet.fraOgMed(input.startdato));
    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPeriode(Arbeidstaker input) {
        if (input == null || input.getArbeidstidInfo() == null || input.getArbeidstidInfo().getPerioder() == null || input.getArbeidstidInfo().getPerioder().isEmpty()) {
            return Collections.emptyList();
        }

        InternArbeidsforholdRef arbeidsforholdRef = null; // får ikke fra søknad, setter default null her, tolker om til InternArbeidsforholdRef.nullRef() ved fastsette uttak.
        var arbeidsgiver = input.getOrganisasjonsnummer() != null
            ? Arbeidsgiver.virksomhet(input.getOrganisasjonsnummer().verdi)
            : (input.getNorskIdentitetsnummer() != null
                ? Arbeidsgiver.fra(new AktørId(input.getNorskIdentitetsnummer().verdi))
                : null);

        var mappedPerioder = input.getArbeidstidInfo().getPerioder().entrySet().stream()
            .map(entry -> {
                Periode k = entry.getKey();
                var v = entry.getValue(); // TODO: skrive omt til entry.getValue().getFaktiskArbeidTimerPerDag()?
                return new UttakAktivitetPeriode(k.getFraOgMed(), k.getTilOgMed(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, null, null);
            })
            .collect(Collectors.toList());
        return mappedPerioder;
    }

    private Ferie mapFerie(LovbestemtFerie input) {
        if (input == null || input.getPerioder() == null || input.getPerioder().isEmpty()) {
            return null;
        }
        var mappedPerioder = input.getPerioder().stream()
            .map(entry -> new FeriePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getFraOgMed(), entry.getTilOgMed())))
            .collect(Collectors.toSet());

        return new Ferie(mappedPerioder);
    }

    private Søknadsperioder mapSøknadsperioder(Collection<Periode> input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        var mappedPerioder = input.stream()
            .map(entry -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getFraOgMed(), entry.getTilOgMed())))
            .collect(Collectors.toSet());

        return new Søknadsperioder(mappedPerioder);
    }

    private static <T> List<T> nullableList(List<T> input) {
        return input != null ? input : Collections.emptyList();
    }
}
