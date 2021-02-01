package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.personopplysninger.Bosteder;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@FagsakYtelseTypeRef
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_KODE)
public class DokumentmottakerSøknadOmsorgspenger implements Dokumentmottaker {

    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private UttakRepository uttakRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    private LagreOppgittOpptjening lagreOppgittOpptjening;
    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;


    private SøknadUtbetalingOmsorgspengerDokumentValidator dokumentValidator;

    DokumentmottakerSøknadOmsorgspenger() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerSøknadOmsorgspenger(BehandlingRepositoryProvider repositoryProvider,
                                        UttakRepository uttakRepository,
                                        BehandlingRepository behandlingRepository,
                                        ProsessTaskRepository prosessTaskRepository,
                                        LagreOppgittOpptjening lagreOppgittOpptjening,
                                        SøknadParser søknadParser,
                                        MottatteDokumentRepository mottatteDokumentRepository,
                                        @Any SøknadUtbetalingOmsorgspengerDokumentValidator dokumentValidator) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.uttakRepository = uttakRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.lagreOppgittOpptjening = lagreOppgittOpptjening;
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.dokumentValidator = dokumentValidator;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        Long behandlingId = behandling.getId();
        dokumentValidator.validerDokumenter(Long.toString(behandlingId), dokumenter);

        var søknader = søknadParser.parseSøknader(dokumenter);
        for (var dokument : dokumenter) {
            var søknad = søknader.get(0);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
        }

        for (Søknad søknad : søknader) {
            persister(søknad, behandling);
        }

        lagreOppgittOpptjeningFraSøknader(behandlingId);
    }

    /**
     * Lagrer inntektsmeldinger til abakus fra mottatt dokument.
     */
    private void lagreOppgittOpptjeningFraSøknader(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        AktørId aktørId = behandling.getAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        var enkeltTask = new ProsessTaskData(LagreOppgittOpptjeningFraSøknadTask.TASKTYPE);
        enkeltTask.setBehandling(behandling.getFagsakId(), behandlingId, aktørId.getId());
        enkeltTask.setSaksnummer(saksnummer.getVerdi());
        enkeltTask.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(enkeltTask);
    }

    void persister(Søknad søknad, Behandling behandling) {
        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        var søknadInnhold = (OmsorgspengerUtbetaling) søknad.getYtelse();

        // Søknad
        lagreSøknad(behandlingId, søknad, søknadInnhold);

        // Medlemskapsinfo
        Bosteder bosteder = null; // TODO: ikke eksponert i kontrakt
        lagreMedlemskapinfo(behandlingId, bosteder, søknad.getMottattDato().toLocalDate());

        // Uttaksperioder og oppgitt opptjening
        lagreUttakOgOpptjening(søknadInnhold, behandling, fagsakId, søknad.getSøker());
    }

    private void lagreSøknad(Long behandlingId, Søknad søknad, OmsorgspengerUtbetaling søknadInnhold) {
        var søknadsperioder = new TreeSet<>(søknadInnhold.getFraværsperioder() == null ? Collections.emptySortedSet() : søknadInnhold.getFraværsperioder().stream().map(FraværPeriode::getPeriode).sorted().collect(Collectors.toList()));
        var maksSøknadsperiode = søknadsperioder.isEmpty() ? null : DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperioder.first().getFraOgMed(), søknadsperioder.last().getTilOgMed());

        final boolean elektroniskSøknad = false;
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(maksSøknadsperiode)
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(søknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(søknad.getMottattDato().toLocalDate()) // TODO: Hva er dette? Dette feltet er datoen det gjelder fra for FP-endringssøknader.
            .medSpråkkode(getSpråkValg(Språk.NORSK_BOKMÅL))
            //.medSpråkkode(getSpraakValg(ytelse.språk)) // TODO: Er ikke språk støttet i søknad lenger?
            ;
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    private void lagreUttakOgOpptjening(OmsorgspengerUtbetaling ytelse, Behandling behandling, Long fagsakId, Søker søker) {
        var behandlingId = behandling.getId();

        // Uttak - basert på frisinn
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        UttakGrunnlag mapUttakGrunnlag;
        if (sisteBehandling.isPresent()) {
            // TODO: Gir det mening å kopiere tidligere fastsatt uttak? => JA, søknader blir delta. Selvbetjening viser ikke tidligere resultat til bruker
            UttakAktivitet uttakAktivitet = uttakRepository.hentFastsattUttak(sisteBehandling.get().getId());
            mapUttakGrunnlag = new MapSøknadUttak(ytelse).lagGrunnlag(behandlingId, uttakAktivitet.getPerioder(), søker);
        } else {
            mapUttakGrunnlag = new MapSøknadUttak(ytelse).lagGrunnlag(behandlingId, Collections.emptySet(), søker);
        }
        uttakRepository.lagreOgFlushNyttGrunnlag(behandlingId, mapUttakGrunnlag);

        // Oppgitt opptjening - lagres i abakus
        lagreOppgittOpptjening.lagreOpptjening(behandling, ZonedDateTime.now(), ytelse);

        // Utvide fagsakperiode
        var maksPeriode = mapUttakGrunnlag.getOppgittUttak().getMaksPeriode();
        fagsakRepository.utvidPeriode(fagsakId, maksPeriode.getFomDato(), maksPeriode.getTomDato());
    }

    private void lagreMedlemskapinfo(Long behandlingId, Bosteder bosteder, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);

        // TODO: Hva skal vi ha som "oppholdNå"?
        // Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        // oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        if (bosteder != null) {
            bosteder.perioder.forEach((periode, opphold) -> {
                // TODO: "tidligereOpphold" må fjernes fra database og domeneobjekter. Ved bruk må skjæringstidspunkt spesifikt oppgis.
                // boolean tidligereOpphold = opphold.getPeriode().getFom().isBefore(mottattDato);
                oppgittTilknytningBuilder.leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                    .medLand(finnLandkode(opphold.land.landkode))
                    .medPeriode(
                        Objects.requireNonNull(periode.getFraOgMed()),
                        Objects.requireNonNullElse(periode.getTilOgMed(), Tid.TIDENES_ENDE))
                    // .erTidligereOpphold(tidligereOpphold)
                    .build());
            });
        }
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private Språkkode getSpråkValg(Språk språk) {
        if (språk != null) {
            return Språkkode.fraKode(språk.dto.toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }

}
