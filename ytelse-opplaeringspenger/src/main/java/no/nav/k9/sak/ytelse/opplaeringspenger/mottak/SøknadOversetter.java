package no.nav.k9.sak.ytelse.opplaeringspenger.mottak;

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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.FeriePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UtenlandsoppholdPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.olp.v1.Opplæringspenger;
import no.nav.k9.søknad.ytelse.olp.v1.kurs.Kurs;
import no.nav.k9.søknad.ytelse.olp.v1.kurs.KursPeriodeMedReisetid;
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
        Opplæringspenger ytelse = søknad.getYtelse();
        var søknadsperioder = ytelse.getSøknadsperiodeList().stream().map(di -> new Periode(di.getFraOgMed(), di.getTilOgMed())).collect(Collectors.toList());

        var arbeidPerioder = new MapSøknadUttakPerioder(tpsTjeneste).mapOppgittArbeidstid(ytelse.getArbeidstid());
        var perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            mapTilUttakPerioder(ytelse.getUttak()),
            arbeidPerioder,
            List.of(),
            mapUtenlandsopphold(ytelse.getUtenlandsopphold()),
            mapFerie(søknadsperioder, ytelse.getLovbestemtFerie()),
            List.of(),
            List.of(),
            mapKurs(ytelse.getKurs()));

        var maksSøknadsperiode = finnMaksperiode(søknadsperioder);

        søknadPersisterer.lagreSøknadEntitet(søknad, journalpostId, behandlingId, maksSøknadsperiode, mottattDato);
        søknadPersisterer.lagreMedlemskapinfo(ytelse.getBosteder(), behandlingId, mottattDato);
        søknadPersisterer.lagrePleietrengende(fagsakId, ytelse.getPleietrengende().getPersonIdent());
        søknadPersisterer.lagreSøknadsperioder(søknadsperioder, ytelse.getTrekkKravPerioder(), journalpostId, behandlingId);
        søknadPersisterer.lagreUttak(perioderFraSøknad, behandlingId);
        søknadPersisterer.oppdaterFagsakperiode(maksSøknadsperiode, fagsakId);
    }

    private List<KursPeriode> mapKurs(Kurs kurs) {
        List<KursPeriodeMedReisetid> kursPerioderMedReisetid = kurs.getKursperioder();
        return kursPerioderMedReisetid.stream().map(kursPeriodeMedReisetid ->
                new KursPeriode(
                    kursPeriodeMedReisetid.getPeriode().getFraOgMed(),
                    kursPeriodeMedReisetid.getPeriode().getTilOgMed(),
                    finnReiseperiodeTil(kursPeriodeMedReisetid),
                    finnReiseperiodeHjem(kursPeriodeMedReisetid),
                    kurs.getKursholder().getInstitusjonUuid(),
                    kursPeriodeMedReisetid.getBegrunnelseReisetidTil(),
                    kursPeriodeMedReisetid.getBegrunnelseReisetidHjem()))
            .collect(Collectors.toList());
    }

    private DatoIntervallEntitet finnReiseperiodeTil(KursPeriodeMedReisetid kursPeriodeMedReisetid) {
        if (kursPeriodeMedReisetid.getAvreise().equals(kursPeriodeMedReisetid.getPeriode().getFraOgMed())) {
            return null;
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(kursPeriodeMedReisetid.getAvreise(), kursPeriodeMedReisetid.getPeriode().getFraOgMed().minusDays(1));
    }

    private DatoIntervallEntitet finnReiseperiodeHjem(KursPeriodeMedReisetid kursPeriodeMedReisetid) {
        if (kursPeriodeMedReisetid.getHjemkomst().equals(kursPeriodeMedReisetid.getPeriode().getTilOgMed())) {
            return null;
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(kursPeriodeMedReisetid.getPeriode().getTilOgMed().plusDays(1), kursPeriodeMedReisetid.getHjemkomst());
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
                    entry.getValue().getÅrsak() == null ? UtenlandsoppholdÅrsak.INGEN : UtenlandsoppholdÅrsak.fraKode(entry.getValue().getÅrsak().name()),
                    entry.getValue().getErSammenMedBarnet()
                )
            )
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
                        entry.getValue().getÅrsak() == null ? UtenlandsoppholdÅrsak.INGEN : UtenlandsoppholdÅrsak.fraKode(entry.getValue().getÅrsak().name()),
                        entry.getValue().getErSammenMedBarnet()
                    )
                )
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

    private Collection<UttakPeriode> mapTilUttakPerioder(Uttak uttak) {
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
