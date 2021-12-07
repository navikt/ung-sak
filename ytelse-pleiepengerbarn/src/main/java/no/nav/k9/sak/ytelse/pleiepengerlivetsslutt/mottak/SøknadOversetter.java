package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Bosteder;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.pls.v1.Pleietrengende;
import no.nav.k9.søknad.ytelse.pls.v1.PleipengerLivetsSluttfase;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;

@Dependent
class SøknadOversetter {

    private SøknadRepository søknadRepository;
    private SøknadsperiodeRepository søknadsperiodeRepository;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private MedlemskapRepository medlemskapRepository;
    private TpsTjeneste tpsTjeneste;
    private FagsakRepository fagsakRepository;
    private boolean skalBrukeUtledetEndringsperiode;

    SøknadOversetter() {
        // for CDI proxy
    }

    @Inject
    SøknadOversetter(SøknadsperiodeRepository søknadsperiodeRepository, BehandlingRepositoryProvider repositoryProvider,
                     UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                     TpsTjeneste tpsTjeneste,
                     @KonfigVerdi(value = "ENABLE_UTLEDET_ENDRINGSPERIODE", defaultVerdi = "false") boolean skalBrukeUtledetEndringsperiode) {
        this.søknadsperiodeRepository = søknadsperiodeRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.tpsTjeneste = tpsTjeneste;
        this.skalBrukeUtledetEndringsperiode = skalBrukeUtledetEndringsperiode;
    }


    void persister(Søknad søknad, JournalpostId journalpostId, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();

        PleiepengerSyktBarn ytelse = søknad.getYtelse();
        // TODO: Tore erstatt med ny søknad
        PleipengerLivetsSluttfase ytelsePls = new PleipengerLivetsSluttfase()
            .medPleietrengende(new Pleietrengende(NorskIdentitetsnummer.of(ytelse.getBarn().getPersonIdent().getVerdi())))
            .medArbeidstid(ytelse.getArbeidstid())
            .medOpptjeningAktivitet(ytelse.getOpptjeningAktivitet())
            .medBosteder(ytelse.getBosteder())
            .medUtenlandsopphold(ytelse.getUtenlandsopphold());

        var mapper = new MapSøknadUttakPerioder(tpsTjeneste, søknad, ytelsePls, journalpostId);
        var perioderFraSøknad = mapper.getPerioderFraSøknad();

        final List<Periode> søknadsperioder= perioderFraSøknad.getArbeidPerioder().stream().map(it -> it.getPeriode()).map(di -> new Periode(di.getFomDato(), di.getTomDato())).collect(Collectors.toList());
        final var maksSøknadsperiode = finnMaksperiode(søknadsperioder);

        // TODO: Stopp barn som mangler norskIdentitetsnummer i k9-punsj ... eller støtt fødselsdato her?

        // TODO etter18feb: Fjern denne fra entitet og DB:
        final boolean elektroniskSøknad = false;

        LocalDate mottattDato = søknad.getMottattDato().toLocalDate();

        // TODO: Hvis vi skal beholde SøknadEntitet trenger vi å lagre SøknadID og sikre idempotens med denne.

        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(maksSøknadsperiode.map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed())).orElse(null))
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(mottattDato)
            .medErEndringssøknad(false) // TODO: Håndtere endringssøknad. "false" betyr at vi krever IMer.
            .medJournalpostId(journalpostId)
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medSøknadsdato(maksSøknadsperiode.map(Periode::getFraOgMed).orElse(mottattDato))
            .medSpråkkode(getSpraakValg(søknad.getSpråk()));
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);

        // Utgår for K9-ytelsene?
        // .medBegrunnelseForSenInnsending(wrapper.getBegrunnelseForSenSoeknad())
        // .medTilleggsopplysninger(wrapper.getTilleggsopplysninger())

        // TODO etter18feb: lagreOpptjeningForSnOgFl(ytelse.getArbeidAktivitet());

        // TODO: Hvorfor er getBosteder() noe annet enn getUtenlandsopphold ??
        lagreMedlemskapinfo(ytelsePls.getBosteder(), behandlingId, mottattDato);

        lagrePleietrengende(fagsakId, ytelsePls.getPleietrengende());

        lagreUttakOgPerioder(søknad, ytelsePls, maksSøknadsperiode, journalpostId, behandlingId, fagsakId);
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

    private List<Periode> hentAlleSøknadsperioder(PleiepengerSyktBarn ytelse) {
        final LocalDateTimeline<Boolean> kompletteSøknadsperioderTidslinje = tilTidslinje(ytelse.getSøknadsperiodeList());
        final var endringsperioder = skalBrukeUtledetEndringsperiode ? ytelse.getUtledetEndringsperiode() : ytelse.getEndringsperiode();
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


    private void lagreUttakOgPerioder(Søknad soknad, PleipengerLivetsSluttfase ytelse, Optional<Periode> maksSøknadsperiode, JournalpostId journalpostId, final Long behandlingId, Long fagsakId) {
        // TODO etter18feb: LovbestemtFerie

        // TODO 18feb: Arbeidstid
        // TODO etter18feb: UttakPeriodeInfo
        var perioderFraSøknad = new MapSøknadUttakPerioder(tpsTjeneste, soknad, ytelse, journalpostId).getPerioderFraSøknad();
        uttakPerioderGrunnlagRepository.lagre(behandlingId, perioderFraSøknad);


        var søknadsperioder = perioderFraSøknad.getArbeidPerioder().stream()
            .map(s -> new Søknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getPeriode().getFomDato(), s.getPeriode().getTomDato())))
            .collect(Collectors.toList());
        søknadsperiodeRepository.lagre(behandlingId, new Søknadsperioder(journalpostId, søknadsperioder));

        maksSøknadsperiode.ifPresent(periode -> fagsakRepository.utvidPeriode(fagsakId, periode.getFraOgMed(), periode.getTilOgMed()));
    }

    private void lagrePleietrengende(Long fagsakId, Pleietrengende pleietrengende) {
        final var norskIdentitetsnummer = pleietrengende.getPersonIdent();
        if (norskIdentitetsnummer != null) {
            final var aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(norskIdentitetsnummer.getVerdi())).orElseThrow();
            fagsakRepository.oppdaterPleietrengende(fagsakId, aktørId);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void lagreMedlemskapinfo(Bosteder bosteder, Long behandlingId, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);

        // TODO: Hva skal vi ha som "oppholdNå"?
        // Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        // oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        if (bosteder != null) {
            bosteder.getPerioder().forEach((periode, opphold) -> {
                // TODO: "tidligereOpphold" må fjernes fra database og domeneobjekter. Ved bruk må skjæringstidspunkt spesifikt oppgis.
                // boolean tidligereOpphold = opphold.getPeriode().getFom().isBefore(mottattDato);
                oppgittTilknytningBuilder.leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                    .medLand(finnLandkode(opphold.getLand().getLandkode()))
                    .medPeriode(
                        Objects.requireNonNull(periode.getFraOgMed()),
                        Objects.requireNonNullElse(periode.getTilOgMed(), Tid.TIDENES_ENDE))
                    // .erTidligereOpphold(tidligereOpphold)
                    .build());
            });
        }
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
