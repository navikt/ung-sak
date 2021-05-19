package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
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
import no.nav.k9.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.OppgittOpptjeningMapper;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_KODE)
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_AT_KODE)
public class DokumentmottakerSøknadOmsorgspenger implements Dokumentmottaker {

    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;
    private FagsakRepository fagsakRepository;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste;

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadOppgittFraværMapper mapper;


    private SøknadUtbetalingOmsorgspengerDokumentValidator dokumentValidator;

    DokumentmottakerSøknadOmsorgspenger() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerSøknadOmsorgspenger(BehandlingRepositoryProvider repositoryProvider,
                                        OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository,
                                        BehandlingRepository behandlingRepository,
                                        ProsessTaskRepository prosessTaskRepository,
                                        OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste,
                                        SøknadParser søknadParser,
                                        MottatteDokumentRepository mottatteDokumentRepository,
                                        SøknadOppgittFraværMapper mapper,
                                        @Any SøknadUtbetalingOmsorgspengerDokumentValidator dokumentValidator) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.omsorgspengerGrunnlagRepository = omsorgspengerGrunnlagRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppgittOpptjeningMapperTjeneste = oppgittOpptjeningMapperTjeneste;
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.mapper = mapper;
        this.dokumentValidator = dokumentValidator;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        Long behandlingId = behandling.getId();
        dokumentValidator.validerDokumenter(behandlingId, dokumenter);

        for (MottattDokument dokument : dokumenter) {
            Søknad søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling, dokument.getJournalpostId());
            // Søknadsinnhold som persisteres eksternt (abakus)
            lagreOppgittOpptjeningFraSøknad(søknad, behandling, dokument);
        }
    }


    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt dokument.
     */
    private void lagreOppgittOpptjeningFraSøknad(Søknad søknad, Behandling behandling, MottattDokument dokument) {
        try {
            OpptjeningAktivitet opptjeningAktiviteter = ((OmsorgspengerUtbetaling) søknad.getYtelse()).getAktivitet();
            var request = oppgittOpptjeningMapperTjeneste.mapRequest(behandling, dokument, opptjeningAktiviteter);
            if (request.getOppgittOpptjening() == null) {
                // Ingenting mer som skal lagres - dokument settes som ferdig
                mottatteDokumentRepository.oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
                return;
            }
            var enkeltTask = new ProsessTaskData(AsyncAbakusLagreOpptjeningTask.TASKTYPE);
            var payload = IayGrunnlagJsonMapper.getMapper().writeValueAsString(request);
            enkeltTask.setPayload(payload);

            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID, dokument.getJournalpostId().getVerdi());
            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.BREVKODER, dokument.getType().getKode());

            enkeltTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
            enkeltTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
            enkeltTask.setCallIdFraEksisterende();

            prosessTaskRepository.lagre(enkeltTask);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Opprettelse av task for lagring av oppgitt opptjening i abakus feiler.", e).toException();
        }
    }

    void persister(Søknad søknad, Behandling behandling, JournalpostId journalpostId) {
        var behandlingId = behandling.getId();
        var søknadInnhold = (OmsorgspengerUtbetaling) søknad.getYtelse();
        var søker = søknad.getSøker();
        var forsendelseMottatt = søknad.getMottattDato().toLocalDate();

        validerStøttetVariant(søknadInnhold);

        lagreSøknad(behandlingId, journalpostId, søknad, søknadInnhold);
        lagreMedlemskapinfo(behandlingId, søknadInnhold, journalpostId, forsendelseMottatt, søker);
        lagreUttakOgUtvidPeriode(behandling, journalpostId, søknadInnhold, søker);
    }

    private void validerStøttetVariant(OmsorgspengerUtbetaling søknadInnhold) {
        if (søknadInnhold.getFosterbarn() != null && !søknadInnhold.getFosterbarn().isEmpty()) {
            throw new UnsupportedOperationException("Variant ikke støttet ennå for søknad omsorgspenger: Fosterbarn");
        }
        if (søknadInnhold.getFraværsperioder().stream().anyMatch(fp -> fp.getDuration() != null)) {
            throw new UnsupportedOperationException("Variant ikke støttet ennå for søknad omsorgspenger: Delvis fravær");
        }
    }

    private void lagreSøknad(Long behandlingId, JournalpostId journalpostId, Søknad søknad, OmsorgspengerUtbetaling søknadInnhold) {
        var søknadsperiode = søknadInnhold.getSøknadsperiode();
        final boolean elektroniskSøknad = false;
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFraOgMed(), søknadsperiode.getTilOgMed()))
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(søknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(søknad.getMottattDato().toLocalDate())
            .medJournalpostId(journalpostId)
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medSpråkkode(getSpråkValg(Språk.NORSK_BOKMÅL)) //TODO: hente riktig språk
            ;
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    private void lagreUttakOgUtvidPeriode(Behandling behandling, JournalpostId journalpostId, OmsorgspengerUtbetaling ytelse, Søker søker) {
        var behandlingId = behandling.getId();
        var fagsakId = behandling.getFagsakId();

        // TODO: vurder om aggregering av perioder kan gjøres smidigere
        Set<OppgittFraværPeriode> søktFravær = new LinkedHashSet<>();
        var søktFraværFraTidligere = omsorgspengerGrunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(behandlingId)
            .map(OppgittFravær::getPerioder)
            .orElse(Set.of());
        var søktFraværFraSøknad = mapper.map(ytelse, søker, journalpostId);
        søktFravær.addAll(søktFraværFraTidligere);
        søktFravær.addAll(søktFraværFraSøknad);

        var fraværFraSøknad = new OppgittFravær(søktFravær);
        omsorgspengerGrunnlagRepository.lagreOgFlushOppgittFraværFraSøknad(behandlingId, fraværFraSøknad);

        // Utvide fagsakperiode
        var maksPeriode = omsorgspengerGrunnlagRepository.hentMaksPeriode(behandlingId).orElseThrow();
        fagsakRepository.utvidPeriode(fagsakId, maksPeriode.getFomDato(), maksPeriode.getTomDato());
    }

    private void lagreMedlemskapinfo(Long behandlingId, OmsorgspengerUtbetaling ytelse, JournalpostId journalpostId, LocalDate forsendelseMottatt, Søker søker) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);
        var bosteder = ytelse.getBosteder();
        if (bosteder != null) {
            var førsteSøkteFraværsdag = finnFørsteSøkteFraværsdag(ytelse, journalpostId, søker);
            bosteder.getPerioder().forEach((periode, opphold) -> {
                var tidligereOpphold = periode.getFraOgMed().isBefore(førsteSøkteFraværsdag);
                oppgittTilknytningBuilder
                    .leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                        .erTidligereOpphold(tidligereOpphold)
                        .medLand(finnLandkode(opphold.getLand().getLandkode()))
                        .medPeriode(
                            Objects.requireNonNull(periode.getFraOgMed()),
                            Objects.requireNonNull(periode.getTilOgMed()))
                        .build());
            });
        }
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private LocalDate finnFørsteSøkteFraværsdag(OmsorgspengerUtbetaling ytelse, JournalpostId journalpostId, Søker søker) {
        var søktFraværFraSøknad = mapper.map(ytelse, søker, journalpostId);
        return søktFraværFraSøknad.stream()
            .map(OppgittFraværPeriode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .sorted()
            .findFirst()
            .orElseThrow();
    }

    private Språkkode getSpråkValg(Språk språk) {
        if (språk != null) {
            return Språkkode.fraKode(språk.getKode().toUpperCase());
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
