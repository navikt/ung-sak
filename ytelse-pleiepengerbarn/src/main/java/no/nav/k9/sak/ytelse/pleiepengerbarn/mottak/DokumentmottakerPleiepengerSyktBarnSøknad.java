package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.abakus.AbakusInntektArbeidYtelseTjenesteFeil;
import no.nav.k9.sak.mottak.dokumentmottak.AsyncAbakusLagreOpptjeningTask;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.dokumentmottak.OppgittOpptjeningMapper;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet;
import no.nav.k9.søknad.felles.type.Journalpost;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnValidator;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
@DokumentGruppeRef(Brevkode.PLEIEPENGER_BARN_SOKNAD_KODE)
class DokumentmottakerPleiepengerSyktBarnSøknad implements Dokumentmottaker {

    private SøknadOversetter pleiepengerBarnSoknadOversetter;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private SøknadParser søknadParser;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste;

    DokumentmottakerPleiepengerSyktBarnSøknad() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerPleiepengerSyktBarnSøknad(MottatteDokumentRepository mottatteDokumentRepository,
                                              SøknadParser søknadParser,
                                              SøknadOversetter pleiepengerBarnSoknadOversetter,
                                              SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer,
                                              ProsessTaskRepository prosessTaskRepository,
                                              OppgittOpptjeningMapper oppgittOpptjeningMapperTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppgittOpptjeningMapperTjeneste = oppgittOpptjeningMapperTjeneste;
    }

    @Override
    public void lagreDokumentinnhold(Collection<MottattDokument> dokumenter, Behandling behandling) {
        var behandlingId = behandling.getId();

        var sorterteDokumenter = sorterSøknadsdokumenter(dokumenter);
        for (MottattDokument dokument : sorterteDokumenter) {
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
            OpptjeningAktivitet opptjeningAktiviteter = ((PleiepengerSyktBarn) søknad.getYtelse()).getOpptjeningAktivitet();
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

    private void persister(Søknad søknad, Behandling behandling, JournalpostId journalpostId) {
        new PleiepengerSyktBarnValidator().forsikreValidert(søknad.getYtelse());

        pleiepengerBarnSoknadOversetter.persister(søknad, journalpostId, behandling);

        for (Journalpost journalpost : søknad.getJournalposter()) {
            boolean journalpostHarInformasjonSomIkkeKanPunsjes = false;
            boolean journalpostHarMedisinskeOpplysninger = true;
            if (journalpost.getInneholderInfomasjonSomIkkeKanPunsjes() != null) {
                journalpostHarInformasjonSomIkkeKanPunsjes = journalpost.getInneholderInfomasjonSomIkkeKanPunsjes();
            }
            if (journalpost.getInneholderMedisinskeOpplysninger() != null) {
                journalpostHarMedisinskeOpplysninger = journalpost.getInneholderMedisinskeOpplysninger();
            }

            // TODO: Fjern hack som retter feilinnsending fra k9-punsj:
            if ("508249437".equals(journalpostId.getVerdi())) {
                continue;
            }
            
            sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(
                behandling,
                journalpostId,
                behandling.getFagsak().getPleietrengendeAktørId(),
                søknad.getMottattDato().toLocalDateTime(),
                journalpostHarInformasjonSomIkkeKanPunsjes,
                journalpostHarMedisinskeOpplysninger);
        }

        Optional<Journalpost> journalpost = søknad.getJournalposter()
            .stream()
            .filter(j -> j.getJournalpostId().equals(journalpostId.getVerdi()))
            .findFirst();

        if (!journalpost.isPresent()) {
            sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(behandling,
                journalpostId,
                behandling.getFagsak().getPleietrengendeAktørId(),
                søknad.getMottattDato().toLocalDateTime(),
                false,
                true);
        }
    }

    @Override
    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
    }
}
