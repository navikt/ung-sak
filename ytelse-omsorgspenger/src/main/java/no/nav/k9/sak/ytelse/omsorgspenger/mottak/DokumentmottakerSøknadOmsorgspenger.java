package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

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
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.Dokumentmottaker;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.søknad.Søknad;
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
    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;

    private SøknadParser søknadParser;
    private MottatteDokumentRepository mottatteDokumentRepository;


    private SøknadUtbetalingOmsorgspengerDokumentValidator dokumentValidator;

    DokumentmottakerSøknadOmsorgspenger() {
        // for CDI proxy
    }

    @Inject
    DokumentmottakerSøknadOmsorgspenger(BehandlingRepositoryProvider repositoryProvider,
                                        OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository,
                                        BehandlingRepository behandlingRepository,
                                        ProsessTaskRepository prosessTaskRepository,
                                        SøknadParser søknadParser,
                                        MottatteDokumentRepository mottatteDokumentRepository,
                                        @Any SøknadUtbetalingOmsorgspengerDokumentValidator dokumentValidator) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.omsorgspengerGrunnlagRepository = omsorgspengerGrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.søknadParser = søknadParser;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
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
        }
        // Søknadsinnhold som persisteres eksternt (abakus)
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

    void persister(Søknad søknad, Behandling behandling, JournalpostId journalpostId) {
        var behandlingId = behandling.getId();
        var søknadInnhold = (OmsorgspengerUtbetaling) søknad.getYtelse();
        var bosteder = ((OmsorgspengerUtbetaling) søknad.getYtelse()).getBosteder();

        lagreSøknad(behandlingId, søknad, søknadInnhold);
        lagreMedlemskapinfo(behandlingId, bosteder, søknad.getMottattDato().toLocalDate());
        lagreUttakOgUtvidPeriode(behandling, journalpostId, søknadInnhold, søknad.getSøker());
    }

    private void lagreSøknad(Long behandlingId, Søknad søknad, OmsorgspengerUtbetaling søknadInnhold) {
        var søknadsperiode = søknadInnhold.getSøknadsperiode();
        final boolean elektroniskSøknad = false;
        var søknadBuilder = new SøknadEntitet.Builder()
            .medSøknadsperiode(DatoIntervallEntitet.fraOgMedTilOgMed(søknadsperiode.getFraOgMed(), søknadsperiode.getFraOgMed()))
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(søknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(søknad.getMottattDato().toLocalDate())
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
        var søktFraværFraSøknad = new SøknadOppgittFraværMapper(ytelse, søker, journalpostId).map();
        søktFravær.addAll(søktFraværFraTidligere);
        søktFravær.addAll(søktFraværFraSøknad);

        var fraværFraSøknad = new OppgittFravær(søktFravær);
        omsorgspengerGrunnlagRepository.lagreOgFlushOppgittFraværFraSøknad(behandlingId, fraværFraSøknad);

        // Utvide fagsakperiode
        var maksPeriode = omsorgspengerGrunnlagRepository.hentGrunnlag(behandlingId)
            .map(grunnlag -> grunnlag.getOppgittFraværFraSøknad().getMaksPeriode())
            .orElseThrow();
        fagsakRepository.utvidPeriode(fagsakId, maksPeriode.getFomDato(), maksPeriode.getTomDato());
    }

    private void lagreMedlemskapinfo(Long behandlingId, Bosteder bosteder, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);
        // TODO: Hva skal vi ha som "oppholdNå"? Er dette relevant for k9 eller bruker vi en annen tolkning for medlemskap?
        // TODO kontrakt har utenlandsopphold, skal dette benyttes?
        if (bosteder != null) {
            bosteder.perioder.forEach((periode, opphold) -> {
                oppgittTilknytningBuilder
                    .leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                    .medLand(finnLandkode(opphold.land.landkode))
                    .medPeriode(
                        Objects.requireNonNull(periode.getFraOgMed()),
                        Objects.requireNonNullElse(periode.getTilOgMed(), Tid.TIDENES_ENDE))
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
