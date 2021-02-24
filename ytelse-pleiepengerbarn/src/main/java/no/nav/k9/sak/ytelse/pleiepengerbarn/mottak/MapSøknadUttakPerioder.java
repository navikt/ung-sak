package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.FeriePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.TilsynsordningPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.LovbestemtFerie;
import no.nav.k9.søknad.felles.aktivitet.Arbeidstaker;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;
import no.nav.k9.søknad.ytelse.psb.v1.Uttak;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo;
import no.nav.k9.søknad.ytelse.psb.v1.tilsyn.TilsynPeriodeInfo;

class MapSøknadUttakPerioder {
    @SuppressWarnings("unused")
    private Søknad søknad;
    private PleiepengerSyktBarn ytelse;
    private JournalpostId journalpostId;

    MapSøknadUttakPerioder(Søknad søknad, JournalpostId journalpostId) {
        this.søknad = søknad;
        this.ytelse = søknad.getYtelse();
        this.journalpostId = journalpostId;
    }

    private static <T> List<T> nullableList(List<T> input) {
        return input != null ? input : Collections.emptyList();
    }

    private Collection<Tilsynsordning> mapOppgittTilsynsordning(no.nav.k9.søknad.ytelse.psb.v1.tilsyn.Tilsynsordning input) {
        if (input == null || input.getPerioder() == null || input.getPerioder().isEmpty()) {
            return List.of();
        }

        var tilsynSvar = Tilsynsordning.OppgittTilsynSvar.JA; // TODO hvordan mappe denne
        var mappedPerioder = input.getPerioder().entrySet().stream()
            .map(entry -> lagTilsynsordningPeriode(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        return List.of(new Tilsynsordning(mappedPerioder, tilsynSvar));
    }

    private TilsynsordningPeriode lagTilsynsordningPeriode(Periode periode, TilsynPeriodeInfo tilsynPeriodeInfo) {
        return new TilsynsordningPeriode(periode.getFraOgMed(), periode.getTilOgMed(), tilsynPeriodeInfo == null ? Duration.ofHours(0) : tilsynPeriodeInfo.getEtablertTilsynTimerPerDag());
    }

    private Collection<ArbeidPeriode> mapOppgittArbeidstid(Arbeidstid arbeidstid) {
        if (arbeidstid == null) {
            return null;
        }
        var mappedUttak = nullableList(arbeidstid.getArbeidstakerList()).stream()
            .flatMap(a -> lagUttakAktivitetPeriode(a, UttakArbeidType.ARBEIDSTAKER).stream())
            .collect(Collectors.toList());
        var mappedFrilanser = mapArbeidstidInfo(arbeidstid.getFrilanserArbeidstidInfo(), UttakArbeidType.FRILANSER);
        var mappedSelvstendigNæringsdrivende = mapArbeidstidInfo(arbeidstid.getSelvstendigNæringsdrivendeArbeidstidInfo(), UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);

        var mappedPerioder = new ArrayList<ArbeidPeriode>();
        mappedPerioder.addAll(mappedUttak);
        mappedPerioder.addAll(mappedFrilanser);
        mappedPerioder.addAll(mappedSelvstendigNæringsdrivende);
        return mappedPerioder;
    }

    private List<ArbeidPeriode> mapArbeidstidInfo(ArbeidstidInfo frilanserArbeidstidInfo, UttakArbeidType arbeidType) {
        if (frilanserArbeidstidInfo == null || frilanserArbeidstidInfo.getPerioder() == null) {
            return List.of();
        }
        return frilanserArbeidstidInfo
            .getPerioder()
            .entrySet()
            .stream()
            .map(entry -> {
                Periode k = entry.getKey();
                return new ArbeidPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(k.getFraOgMed(), k.getTilOgMed()),
                    arbeidType,
                    null,
                    null,
                    frilanserArbeidstidInfo.getJobberNormaltTimerPerDag(), entry.getValue().getFaktiskArbeidTimerPerDag()
                );
            })
            .collect(Collectors.toList());
    }

    private Collection<ArbeidPeriode> lagUttakAktivitetPeriode(Arbeidstaker input, UttakArbeidType aktivitetType) {
        if (input == null || input.getArbeidstidInfo() == null || input.getArbeidstidInfo().getPerioder() == null || input.getArbeidstidInfo().getPerioder().isEmpty()) {
            return Collections.emptyList();
        }

        var arbeidsforholdRef = mapArbeidsforholdRef(aktivitetType); // får ikke fra søknad, setter default InternArbeidsforholdRef.nullRef() her, tolker om til  ved fastsette uttak.
        var arbeidsgiver = mapArbeidsgiver(aktivitetType, input);

        return input.getArbeidstidInfo()
            .getPerioder()
            .entrySet()
            .stream()
            .map(entry -> {
                Periode k = entry.getKey();
                return new ArbeidPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(k.getFraOgMed(), k.getTilOgMed()),
                    aktivitetType,
                    arbeidsgiver,
                    arbeidsforholdRef,
                    input.getArbeidstidInfo().getJobberNormaltTimerPerDag(), entry.getValue().getFaktiskArbeidTimerPerDag()
                );
            })
            .collect(Collectors.toList());
    }

    private Arbeidsgiver mapArbeidsgiver(UttakArbeidType aktivitetType, Arbeidstaker input) {
        if (UttakArbeidType.ARBEIDSTAKER.equals(aktivitetType)) {
            return input.getOrganisasjonsnummer() != null
                ? Arbeidsgiver.virksomhet(input.getOrganisasjonsnummer().verdi)
                : (input.getNorskIdentitetsnummer() != null
                ? Arbeidsgiver.fra(new AktørId(input.getNorskIdentitetsnummer().verdi))
                : null);
        }
        return null;
    }

    private InternArbeidsforholdRef mapArbeidsforholdRef(UttakArbeidType aktivitetType) {
        if (UttakArbeidType.ARBEIDSTAKER.equals(aktivitetType)) {
            return InternArbeidsforholdRef.nullRef();
        }
        return null;
    }

    private Collection<FeriePeriode> mapFerie(LovbestemtFerie input) {
        if (input == null || input.getPerioder() == null || input.getPerioder().isEmpty()) {
            return List.of();
        }

        return input.getPerioder().stream()
            .map(entry -> new FeriePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(entry.getFraOgMed(), entry.getTilOgMed())))
            .collect(Collectors.toSet());
    }

    private Collection<UttakPeriode> mapUttak(Uttak uttak) {
        if (uttak == null || uttak.getPerioder() == null) {
            return List.of();
        }
        return uttak.getPerioder()
            .entrySet()
            .stream()
            .map(it -> new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getKey().getFraOgMed(), it.getKey().getTilOgMed()), it.getValue().getTimerPleieAvBarnetPerDag())).collect(Collectors.toList());
    }

    public PerioderFraSøknad getPerioderFraSøknad() {
        var arbeidperioder = mapOppgittArbeidstid(ytelse.getArbeidstid());
        var tilsynsordning = mapOppgittTilsynsordning(ytelse.getTilsynsordning());
        var uttaksperioder = mapUttak(ytelse.getUttak());
        var ferie = mapFerie(ytelse.getLovbestemtFerie());

        return new PerioderFraSøknad(journalpostId, uttaksperioder, arbeidperioder, tilsynsordning, ferie);
    }

}
