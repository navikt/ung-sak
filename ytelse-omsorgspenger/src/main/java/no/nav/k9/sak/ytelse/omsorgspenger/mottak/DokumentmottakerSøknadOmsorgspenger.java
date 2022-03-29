package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
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
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.Språk;
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_KODE)
@DokumentGruppeRef(Brevkode.SØKNAD_UTBETALING_OMS_AT_KODE)
@DokumentGruppeRef(Brevkode.FRAVÆRSKORRIGERING_IM_OMS_KODE)
public class DokumentmottakerSøknadOmsorgspenger implements Dokumentmottaker {

    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;
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
                                        OmsorgspengerGrunnlagRepository grunnlagRepository,
                                        ProsessTaskTjeneste taskTjeneste,
                                        OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste,
                                        SøknadParser søknadParser,
                                        MottatteDokumentRepository mottatteDokumentRepository,
                                        SøknadOppgittFraværMapper mapper,
                                        @Any SøknadUtbetalingOmsorgspengerDokumentValidator dokumentValidator) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.grunnlagRepository = grunnlagRepository;
        this.taskTjeneste = taskTjeneste;
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

        var sorterteDokumenter = sorterSøknadsdokumenter(dokumenter);
        for (MottattDokument dokument : sorterteDokumenter) {
            Søknad søknad = søknadParser.parseSøknad(dokument);
            dokument.setBehandlingId(behandlingId);
            dokument.setInnsendingstidspunkt(søknad.getMottattDato().toLocalDateTime());
            mottatteDokumentRepository.lagre(dokument, DokumentStatus.BEHANDLER);
            // Søknadsinnhold som persisteres "lokalt" i k9-sak
            persister(søknad, behandling, dokument);
            // Søknadsinnhold som persisteres eksternt (abakus)
            lagreOppgittOpptjeningFraSøknad(søknad, behandling, dokument);
        }
    }

    private LinkedHashSet<MottattDokument> sorterSøknadsdokumenter(Collection<MottattDokument> dokumenter) {
        return dokumenter
            .stream()
            .sorted(Comparator.comparing(MottattDokument::getMottattTidspunkt))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Lagrer oppgitt opptjening til abakus fra mottatt dokument.
     */
    private void lagreOppgittOpptjeningFraSøknad(Søknad søknad, Behandling behandling, MottattDokument dokument) {
        try {
            var utbetaling = (OmsorgspengerUtbetaling) søknad.getYtelse();
            var request = Optional.ofNullable(utbetaling.getAktivitet())
                .map(aktiviteter -> oppgittOpptjeningMapperTjeneste.mapRequest(behandling, dokument, aktiviteter))
                .orElse(null);
            if (request == null || request.getOppgittOpptjening() == null) {
                // Ingenting mer som skal lagres - dokument settes som ferdig
                mottatteDokumentRepository.oppdaterStatus(List.of(dokument), DokumentStatus.GYLDIG);
                return;
            }

            var enkeltTask = ProsessTaskData.forProsessTask(AsyncAbakusLagreOpptjeningTask.class);
            var payload = IayGrunnlagJsonMapper.getMapper().writeValueAsString(request);
            enkeltTask.setPayload(payload);

            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.JOURNALPOST_ID, dokument.getJournalpostId().getVerdi());
            enkeltTask.setProperty(AsyncAbakusLagreOpptjeningTask.BREVKODER, dokument.getType().getKode());

            enkeltTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getAktørId());
            enkeltTask.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
            enkeltTask.setCallIdFraEksisterende();

            taskTjeneste.lagre(enkeltTask);
        } catch (IOException e) {
            throw AbakusInntektArbeidYtelseTjenesteFeil.FEIL.feilVedKallTilAbakus("Opprettelse av task for lagring av oppgitt opptjening i abakus feiler.", e).toException();
        }
    }

    void persister(Søknad søknad, Behandling behandling, MottattDokument dokument) {
        var journalpostId = dokument.getJournalpostId();
        var behandlingId = behandling.getId();
        var søknadInnhold = (OmsorgspengerUtbetaling) søknad.getYtelse();
        var søker = søknad.getSøker();
        var forsendelseMottatt = søknad.getMottattDato().toLocalDate();

        lagreSøknad(behandlingId, dokument, søknad, søknadInnhold);
        lagreMedlemskapinfo(behandlingId, søknadInnhold, forsendelseMottatt);
        lagreUttakOgUtvidPeriode(behandling, journalpostId, søknadInnhold, søker);
    }

    private void lagreSøknad(Long behandlingId, MottattDokument dokument, Søknad søknad, OmsorgspengerUtbetaling søknadInnhold) {
        if (dokument.getType() == Brevkode.FRAVÆRSKORRIGERING_IM_OMS) {
            // Dokument er relatert til eksisterende inntektsmelding, klassifiseres derfor ikke som søknad fra bruker
            return;
        }
        var søknadsperiode = søknadInnhold.getSøknadsperiode();
        final boolean elektroniskSøknad = false;
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFraOgMed(), søknadsperiode.getTilOgMed()))
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(søknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(søknad.getMottattDato().toLocalDate())
            .medJournalpostId(dokument.getJournalpostId())
            .medSøknadId(søknad.getSøknadId() == null ? null : søknad.getSøknadId().getId())
            .medSpråkkode(getSpråkValg(Språk.NORSK_BOKMÅL)) //TODO: hente riktig språk
            ;
        var søknadEntitet = søknadBuilder.build();
        søknadRepository.lagreOgFlush(behandlingId, søknadEntitet);
    }

    private void lagreUttakOgUtvidPeriode(Behandling behandling, JournalpostId journalpostId, OmsorgspengerUtbetaling ytelse, Søker søker) {
        var behandlingId = behandling.getId();
        var fagsakId = behandling.getFagsakId();

        var fraværFraSøknad = mapper.mapFraværFraSøknad(journalpostId, ytelse, søker, behandling);
        grunnlagRepository.lagreOgFlushOppgittFraværFraSøknad(behandlingId, fraværFraSøknad);

        var fraværskorrigeringerIm = mapper.mapFraværskorringeringIm(journalpostId, ytelse, behandling);
        grunnlagRepository.lagreOgFlushFraværskorrigeringerIm(behandlingId, fraværskorrigeringerIm);

        // Utvide fagsakperiode
        var maksPeriode = grunnlagRepository.hentMaksPeriode(behandlingId).orElseThrow();
        fagsakRepository.utvidPeriode(fagsakId, maksPeriode.getFomDato(), maksPeriode.getTomDato());
    }

    private void lagreMedlemskapinfo(Long behandlingId, OmsorgspengerUtbetaling ytelse, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);
        var bosteder = ytelse.getBosteder();
        if (bosteder != null) {
            bosteder.getPerioder().forEach((periode, opphold) -> {
                oppgittTilknytningBuilder
                    .leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                        .medLand(finnLandkode(opphold.getLand().getLandkode()))
                        .medPeriode(
                            Objects.requireNonNull(periode.getFraOgMed()),
                            Objects.requireNonNull(periode.getTilOgMed()))
                        .build());
            });
        }
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
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
    public BehandlingÅrsakType getBehandlingÅrsakType(Brevkode brevkode) {
        return switch (brevkode.getKode()) {
            case Brevkode.FRAVÆRSKORRIGERING_IM_OMS_KODE -> BehandlingÅrsakType.RE_FRAVÆRSKORRIGERING_FRA_SAKSBEHANDLER;
            default -> BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
        };
    }

}
