package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.BeredskapPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.FeriePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.NattevåkPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstid;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.ArbeidstidInfo;

class MapSøknadUttakPerioder {
    private TpsTjeneste tpsTjeneste;
    @SuppressWarnings("unused")
    private Søknad søknad;
    private PleipengerLivetsSluttfase ytelse;
    private JournalpostId journalpostId;

    MapSøknadUttakPerioder(TpsTjeneste tpsTjeneste, Søknad søknad, PleipengerLivetsSluttfase ytelse, JournalpostId journalpostId) {
        this.tpsTjeneste = tpsTjeneste;
        this.søknad = søknad;
        this.ytelse = ytelse;
        this.journalpostId = journalpostId;
    }

    private static <T> List<T> nullableList(List<T> input) {
        return input != null ? input : Collections.emptyList();
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

    private List<ArbeidPeriode> mapArbeidstidInfo(Optional<ArbeidstidInfo> arbeidstidInfo, UttakArbeidType arbeidType) {
        if (arbeidstidInfo.isEmpty()) {
            return List.of();
        }
        return arbeidstidInfo.get()
            .getPerioder()
            .entrySet()
            .stream()
            .map(entry -> {
                Periode k = entry.getKey();
                return new ArbeidPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(k.getFraOgMed(), k.getTilOgMed()),
                    arbeidType,
                    null,
                    null,
                    entry.getValue().getJobberNormaltTimerPerDag(), entry.getValue().getFaktiskArbeidTimerPerDag()
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
                    entry.getValue().getJobberNormaltTimerPerDag(), entry.getValue().getFaktiskArbeidTimerPerDag()
                );
            })
            .collect(Collectors.toList());
    }

    private Arbeidsgiver mapArbeidsgiver(UttakArbeidType aktivitetType, Arbeidstaker input) {
        if (UttakArbeidType.ARBEIDSTAKER.equals(aktivitetType)) {
            return input.getOrganisasjonsnummer() != null
                ? Arbeidsgiver.virksomhet(input.getOrganisasjonsnummer().getVerdi())
                : (input.getNorskIdentitetsnummer() != null
                ? Arbeidsgiver.fra(mapAktørId(input))
                : null);
        }
        return null;
    }

    private AktørId mapAktørId(Arbeidstaker input) {
        return tpsTjeneste.hentAktørForFnr(PersonIdent.fra(input.getNorskIdentitetsnummer().getVerdi())).orElseThrow();
    }

    private InternArbeidsforholdRef mapArbeidsforholdRef(UttakArbeidType aktivitetType) {
        if (UttakArbeidType.ARBEIDSTAKER.equals(aktivitetType)) {
            return InternArbeidsforholdRef.nullRef();
        }
        return null;
    }

    public PerioderFraSøknad getPerioderFraSøknad() {
        var arbeidperioder = mapOppgittArbeidstid(ytelse.getArbeidstid());
        Collection<Tilsynsordning> tilsynsordning = List.of();
        Collection<UttakPeriode> uttaksperioder = arbeidperioder.stream()
            .map(arbeidPeriode -> {
                // TODO PLS: Fikse arbedstid slik at den passer dette formatet
                var jobberNormaltTimerPerDag = arbeidPeriode.getJobberNormaltTimerPerDag();
                var faktiskArbeidTimerPerDag = arbeidPeriode.getFaktiskArbeidTimerPerDag();
                var oppgittTilsyn = jobberNormaltTimerPerDag.minus(faktiskArbeidTimerPerDag); // aka. getTimerPleieAvBarnetPerDag
                return new UttakPeriode(arbeidPeriode.getPeriode(), oppgittTilsyn);
            })
            .collect(Collectors.toList());
        Collection<FeriePeriode> ferie = List.of();
        List<BeredskapPeriode> beredskap = List.of();
        List<NattevåkPeriode> nattevåk = List.of();

        return new PerioderFraSøknad(journalpostId, uttaksperioder, arbeidperioder, tilsynsordning, ferie, beredskap, nattevåk);
    }

}
