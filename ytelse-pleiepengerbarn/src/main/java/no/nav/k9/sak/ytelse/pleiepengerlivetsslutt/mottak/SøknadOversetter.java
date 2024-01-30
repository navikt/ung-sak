package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.MapSøknadUttakPerioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.mottak.SøknadPersisterer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.FeriePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtenlandsoppholdPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;
import no.nav.k9.søknad.ytelse.psb.v1.LovbestemtFerie;
import no.nav.k9.søknad.ytelse.psb.v1.Uttak;

@Dependent
class SøknadOversetter {

    private TpsTjeneste tpsTjeneste;
    private SøknadPersisterer søknadPersisterer;

    @Inject
    SøknadOversetter(TpsTjeneste tpsTjeneste, SøknadPersisterer søknadPersisterer) {
        this.tpsTjeneste = tpsTjeneste;
        this.søknadPersisterer = søknadPersisterer;
    }

    void persister(Søknad søknad, JournalpostId journalpostId, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var mottattDato = søknad.getMottattDato().toLocalDate();
        PleipengerLivetsSluttfase ytelse = søknad.getYtelse();

        Collection<ArbeidPeriode> arbeidPerioder = new MapSøknadUttakPerioder(tpsTjeneste).mapOppgittArbeidstid(ytelse.getArbeidstid());
        final List<Periode> søknadsperioder = hentAlleSøknadsperioder(ytelse);
        Collection<UttakPeriode> uttakPerioder = mapUttak(ytelse.getUttak());

        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            uttakPerioder,
            arbeidPerioder,
            List.of(),
            mapUtenlandsopphold(ytelse.getUtenlandsopphold()),
            mapFerie(søknadsperioder, ytelse.getLovbestemtFerie()),
            List.of(),
            List.of());

        var maksSøknadsperiode = finnMaksperiode(søknadsperioder);

        søknadPersisterer.lagreSøknadEntitet(søknad, journalpostId, behandlingId, maksSøknadsperiode, mottattDato);
        søknadPersisterer.lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);
        søknadPersisterer.lagrePleietrengende(fagsakId, ytelse.getPleietrengende().getPersonIdent());
        søknadPersisterer.lagreSøknadsperioder(søknadsperioder, ytelse.getTrekkKravPerioder(), journalpostId, behandlingId);
        søknadPersisterer.lagreUttak(perioderFraSøknad, behandlingId);
        søknadPersisterer.oppdaterFagsakperiode(maksSøknadsperiode, fagsakId);
    }

    private List<UtenlandsoppholdPeriode> mapUtenlandsopphold(Utenlandsopphold utenlandsopphold) {
        final List<UtenlandsoppholdPeriode> utenlandsoppholdPerioder = utenlandsopphold.getPerioder()
            .entrySet()
            .stream()
            .map(entry ->
                new UtenlandsoppholdPeriode(
                    entry.getKey().getFraOgMed(),
                    entry.getKey().getTilOgMed(),
                    true,
                    Landkoder.fraKode(entry.getValue().getLand().getLandkode()),
                    entry.getValue().getÅrsak() == null ? UtenlandsoppholdÅrsak.INGEN : UtenlandsoppholdÅrsak.fraKode(entry.getValue().getÅrsak().name())))
            .collect(Collectors.toList());

        if (utenlandsopphold.getPerioderSomSkalSlettes() != null) {
            utenlandsoppholdPerioder.addAll(utenlandsopphold.getPerioderSomSkalSlettes()
                .entrySet()
                .stream()
                .map(entry ->
                    new UtenlandsoppholdPeriode(
                        entry.getKey().getFraOgMed(),
                        entry.getKey().getTilOgMed(),
                        false,
                        Landkoder.fraKode(entry.getValue().getLand().getLandkode()),
                        entry.getValue().getÅrsak() == null ? UtenlandsoppholdÅrsak.INGEN : UtenlandsoppholdÅrsak.fraKode(entry.getValue().getÅrsak().name())))
                .collect(Collectors.toList()));
        }
        return utenlandsoppholdPerioder;
    }

    private Collection<FeriePeriode> mapFerie(List<Periode> søknadsperioder, LovbestemtFerie input) {
        LocalDateTimeline<Boolean> ferieTidslinje = toFerieTidslinje(input.getPerioder());

        /*
         * XXX: Dette er en hack. Vi bør endre til at man for søknadsperioder alltid sender inn en komplett liste med både ferieperioder
         *      man skal ha ... og hvilke som skal fjernes.
         */
        ferieTidslinje = ferieTidslinje.combine(toFerieTidslinje(søknadsperioder, false), StandardCombinators::coalesceLeftHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return ferieTidslinje
            .compress()
            .stream()
            .map(s -> new FeriePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
            .collect(Collectors.toList());
    }

    private LocalDateTimeline<Boolean> toFerieTidslinje(Map<Periode, LovbestemtFerie.LovbestemtFeriePeriodeInfo> perioder) {
        return new LocalDateTimeline<>(perioder.entrySet()
            .stream()
            .map(entry -> new LocalDateSegment<>(entry.getKey().getFraOgMed(), entry.getKey().getTilOgMed(), entry.getValue() == null || entry.getValue().isSkalHaFerie()))
            .collect(Collectors.toList())
        );
    }

    private LocalDateTimeline<Boolean> toFerieTidslinje(Collection<Periode> perioder, boolean skalHaFerie) {
        return new LocalDateTimeline<>(perioder
            .stream()
            .map(entry -> new LocalDateSegment<>(entry.getFraOgMed(), entry.getTilOgMed(), skalHaFerie))
            .collect(Collectors.toList())
        );
    }

    private List<Periode> hentAlleSøknadsperioder(PleipengerLivetsSluttfase ytelse) {
        final LocalDateTimeline<Boolean> kompletteSøknadsperioderTidslinje = tilTidslinje(ytelse.getSøknadsperiodeList());
        final var endringsperioder = ytelse.getEndringsperiode();
        final LocalDateTimeline<Boolean> endringssøknadsperioderTidslinje = tilTidslinje(endringsperioder);
        final LocalDateTimeline<Boolean> søknadsperioder = kompletteSøknadsperioderTidslinje.union(endringssøknadsperioderTidslinje, StandardCombinators::coalesceLeftHandSide).compress();
        return søknadsperioder.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());
    }

    private LocalDateTimeline<Boolean> tilTidslinje(List<Periode> perioder) {
        return new LocalDateTimeline<>(
            perioder.stream()
                .map(p -> new LocalDateSegment<>(p.getFraOgMed(), p.getTilOgMed(), Boolean.TRUE))
                .collect(Collectors.toList())
        ).compress();
    }

    Collection<UttakPeriode> mapUttak(Uttak uttak) {
        if (uttak == null || uttak.getPerioder() == null) {
            return List.of();
        }
        return uttak.getPerioder()
            .entrySet()
            .stream()
            .map(it -> new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getKey().getFraOgMed(), it.getKey().getTilOgMed()), it.getValue().getTimerPleieAvBarnetPerDag())).collect(Collectors.toList());
    }


    private Optional<Periode> finnMaksperiode(List<Periode> perioder) {
        if (perioder == null || perioder.isEmpty()) {
            return Optional.empty();
        }
        final var fom = perioder
            .stream()
            .map(Periode::getFraOgMed)
            .min(LocalDate::compareTo)
            .orElseThrow();
        final var tom = perioder
            .stream()
            .map(Periode::getTilOgMed)
            .max(LocalDate::compareTo)
            .orElseThrow();
        return Optional.of(new Periode(fom, tom));
    }
}
