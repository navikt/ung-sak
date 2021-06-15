package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.BeredskapPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.FeriePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.NattevåkPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.Tilsynsordning;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.TilsynsordningPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.psb.v1.Beredskap;
import no.nav.k9.søknad.ytelse.psb.v1.LovbestemtFerie;
import no.nav.k9.søknad.ytelse.psb.v1.Nattevåk;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;
import no.nav.k9.søknad.ytelse.psb.v1.Uttak;
import no.nav.k9.søknad.ytelse.psb.v1.arbeidstid.Arbeidstaker;
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
                ? Arbeidsgiver.fra(new AktørId(input.getNorskIdentitetsnummer().getVerdi()))
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

    private Collection<FeriePeriode> mapFerie(List<Periode> søknadsperioder, LovbestemtFerie input) {
        LocalDateTimeline<Boolean> ferieTidslinje = toFerieTidslinje(input.getPerioder().keySet(), true);
        
        /*
         * XXX: Dette er en hack. Vi bør endre til at man for søknadsperioder alltid sender inn en komplett liste med både ferieperioder
         *      man skal ha ... og hvilke som skal fjernes.
         */
        if (input.getPerioderSomSkalSlettes() != null) {
            ferieTidslinje = ferieTidslinje.combine(toFerieTidslinje(input.getPerioderSomSkalSlettes().keySet(), false), StandardCombinators::coalesceLeftHandSide, JoinStyle.CROSS_JOIN);
        }
        ferieTidslinje = ferieTidslinje.combine(toFerieTidslinje(søknadsperioder, false), StandardCombinators::coalesceLeftHandSide, JoinStyle.CROSS_JOIN);
        
        return ferieTidslinje
                .compress()
                .stream()
                .map(s -> new FeriePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
                .collect(Collectors.toList());
    }

    private LocalDateTimeline<Boolean> toFerieTidslinje(Collection<Periode> perioder, boolean skalHaFerie) {
         return new LocalDateTimeline<>(perioder
                .stream()
                .map(entry -> new LocalDateSegment<>(entry.getFraOgMed(), entry.getTilOgMed(), skalHaFerie))
                .collect(Collectors.toList())
                );
    }
    
    private List<BeredskapPeriode> mapBeredskap(Beredskap beredskap) {
        final List<BeredskapPeriode> beredskapsperioder = beredskap.getPerioder()
                .entrySet()
                .stream()
                .map(entry -> new BeredskapPeriode(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed(), true, entry.getValue().getTilleggsinformasjon()))
                .collect(Collectors.toList());
        
        if (beredskap.getPerioderSomSkalSlettes() != null) {
            beredskapsperioder.addAll(beredskap.getPerioderSomSkalSlettes()
                    .entrySet()
                    .stream()
                    .map(entry -> new BeredskapPeriode(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed(), false, entry.getValue().getTilleggsinformasjon()))
                    .collect(Collectors.toList()));
        }
        
        return beredskapsperioder;
    }
    
    private List<NattevåkPeriode> mapNattevåk(Nattevåk nattevåk) {
        final List<NattevåkPeriode> nattevåkperioder = nattevåk.getPerioder()
                .entrySet()
                .stream()
                .map(entry -> new NattevåkPeriode(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed(), true, entry.getValue().getTilleggsinformasjon()))
                .collect(Collectors.toList());
        
        if (nattevåk.getPerioderSomSkalSlettes() != null) {
            nattevåkperioder.addAll(nattevåk.getPerioderSomSkalSlettes()
                    .entrySet()
                    .stream()
                    .map(entry -> new NattevåkPeriode(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed(), false, entry.getValue().getTilleggsinformasjon()))
                    .collect(Collectors.toList()));
        }
        
        return nattevåkperioder;
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
        var ferie = mapFerie(ytelse.getSøknadsperiodeList(), ytelse.getLovbestemtFerie());
        var beredskap = mapBeredskap(ytelse.getBeredskap());
        var nattevåk = mapNattevåk(ytelse.getNattevåk());

        return new PerioderFraSøknad(journalpostId, uttaksperioder, arbeidperioder, tilsynsordning, ferie, beredskap, nattevåk);
    }

}
