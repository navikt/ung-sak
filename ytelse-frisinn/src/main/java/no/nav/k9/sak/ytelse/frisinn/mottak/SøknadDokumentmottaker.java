package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;

@Dependent
public class SøknadDokumentmottaker {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private SaksnummerRepository saksnummerRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private LagreSøknad soknadOversetter;
    private FagsakTjeneste fagsakTjeneste;
    private LagreOppgittOpptjening lagreOppgittOpptjening;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;

    SøknadDokumentmottaker() {
        // for CDI proxy
    }

    @Inject
    public SøknadDokumentmottaker(DokumentmottakerFelles dokumentmottakerFelles,
                                  SaksnummerRepository saksnummerRepository,
                                  Behandlingsoppretter behandlingsoppretter,
                                  LagreSøknad søknadOversetter,
                                  LagreOppgittOpptjening lagreOppgittOpptjening,
                                  FagsakTjeneste fagsakTjeneste,
                                  BehandlingRepositoryProvider provider) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.søknadRepository = provider.getSøknadRepository();
        this.soknadOversetter = søknadOversetter;
        this.lagreOppgittOpptjening = lagreOppgittOpptjening;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    Fagsak finnEllerOpprett(FagsakYtelseType fagsakYtelseType, AktørId brukerIdent, AktørId pleietrengendeAktørId, LocalDate startDato) {
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(fagsakYtelseType, brukerIdent, pleietrengendeAktørId, startDato, startDato);
        if (fagsak.isPresent()) {
            return fagsak.get();
        }
        LocalDate yearFom = startDato.withDayOfYear(1);
        LocalDate yearTom = startDato.withMonth(12).withDayOfMonth(31);
        var saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        return opprettSakFor(saksnummer, brukerIdent, pleietrengendeAktørId, fagsakYtelseType, yearFom, yearTom);
    }

    Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, FrisinnSøknad søknad) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknad);

        final Behandling behandling = tilknyttBehandling(saksnummer, journalpostId, søknad);
        return behandling;
    }

    private Behandling tilknyttBehandling(Saksnummer saksnummer, JournalpostId journalpostId, FrisinnSøknad søknad) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true).orElseThrow();
        var periode = søknad.getSøknadsperiode();
        var forrigeBehandling = finnForrigeAvsluttedeBehandling(fagsak);

        validerSøknadErForNyPeriode(fagsak, periode.getFraOgMed());
        validerIngenÅpneBehandlinger(fagsak);

        Behandling behandling;
        if (forrigeBehandling.isEmpty()) {
            behandling = opprettNyBehandling(journalpostId, søknad, fagsak);
        } else {
            behandling = opprettEndringBehandling(journalpostId, søknad, forrigeBehandling.get());
        }

        lagreOppgittOpptjening.lagreOpptjening(behandling, søknad.getInntekter(), søknad.getMottattDato());

        return behandling;
    }

    private Optional<Behandling> finnForrigeAvsluttedeBehandling(Fagsak fagsak) {
        // Den sist avsluttede behandlingen er mest korrekt ift. HISTORISKE inntektsdata som skal videreføres. Derfor
        // brukes sist avsluttede behandling fremfor behandling for sist søknadsperiode.
        // GJELDENDE inntektsdata kommer med selve søknaden.
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
    }

    private void validerSøknadErForNyPeriode(Fagsak fagsak, LocalDate fraOgMed) {
        søknadRepository.hentBehandlingerMedOverlappendeSøknaderIPeriode(fagsak.getId(), fraOgMed, LocalDate.now().plusYears(100)).stream()
            .findAny()
            .ifPresent(behandling -> { throw new IllegalStateException("Fant tidligere Frisinn-søknad som overlapper samme periode " +
                "for saksnummer " + fagsak.getSaksnummer() + " fra og med " + fraOgMed); });
    }

    private void validerIngenÅpneBehandlinger(Fagsak fagsak) {
        if (behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).size() > 0) {
            throw new UnsupportedOperationException("Frisinn støtter ikke mottak av søknad for åpne behandlinger, saksnummer = " + fagsak.getSaksnummer());
        }
    }

    private Behandling opprettEndringBehandling(JournalpostId journalpostId, FrisinnSøknad søknad, Behandling forrigeBehandling) {
        Behandling behandling;
        behandling = behandlingsoppretter.opprettRevurdering(forrigeBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        soknadOversetter.persister(søknad, behandling);
        dokumentmottakerFelles.opprettTaskForÅStarteBehandlingMedNySøknad(behandling, journalpostId);
        return behandling;
    }

    private Behandling opprettNyBehandling(JournalpostId journalpostId, FrisinnSøknad søknad, Fagsak fagsak) {
        Behandling behandling;
        // førstegangssøknad
        behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        soknadOversetter.persister(søknad, behandling);
        dokumentmottakerFelles.opprettTaskForÅStarteBehandlingMedNySøknad(behandling, journalpostId);
        return behandling;
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
