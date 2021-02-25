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
import no.nav.vedtak.konfig.KonfigVerdi;

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

    private Boolean flereSøknaderSammeDag;

    SøknadDokumentmottaker() {
        // for CDI proxy
    }

    @Inject
    public SøknadDokumentmottaker(@KonfigVerdi(value = "FRISINN_FLERE_SOKNADER_SAMME_DAG", defaultVerdi = "true") Boolean flereSøknaderSammeDag,
                                  DokumentmottakerFelles dokumentmottakerFelles,
                                  SaksnummerRepository saksnummerRepository,
                                  Behandlingsoppretter behandlingsoppretter,
                                  LagreSøknad søknadOversetter,
                                  LagreOppgittOpptjening lagreOppgittOpptjening,
                                  FagsakTjeneste fagsakTjeneste,
                                  BehandlingRepositoryProvider provider) {
        this.flereSøknaderSammeDag = flereSøknaderSammeDag;
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.søknadRepository = provider.getSøknadRepository();
        this.soknadOversetter = søknadOversetter;
        this.lagreOppgittOpptjening = lagreOppgittOpptjening;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    Fagsak finnEllerOpprett(FagsakYtelseType fagsakYtelseType, AktørId brukerIdent, LocalDate startDato, @SuppressWarnings("unused") LocalDate sluttDato) {
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(fagsakYtelseType, brukerIdent, null, null, startDato, startDato);
        if (fagsak.isPresent()) {
            return fagsak.get();
        }
        LocalDate yearFom = startDato.withDayOfYear(1);
        LocalDate yearTom = startDato.withMonth(12).withDayOfMonth(31);
        var saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        return opprettSakFor(saksnummer, brukerIdent, fagsakYtelseType, yearFom, yearTom);
    }

    Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, FrisinnSøknad søknad) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknad);

        final Behandling behandling = tilknyttBehandling(saksnummer, journalpostId, søknad);
        return behandling;
    }

    private Behandling tilknyttBehandling(Saksnummer saksnummer, JournalpostId journalpostId, FrisinnSøknad søknad) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true).orElseThrow();
        var forrigeBehandling = finnForrigeAvsluttedeBehandling(fagsak);

        validerSøknad(fagsak, søknad);

        Behandling behandling;
        if (forrigeBehandling.isEmpty()) {
            behandling = opprettNyBehandling(journalpostId, søknad, fagsak);
        } else {
            behandling = opprettEndringBehandling(journalpostId, søknad, forrigeBehandling.get());
        }

        lagreOppgittOpptjening.lagreOpptjening(behandling, søknad.getInntekter(), søknad.getMottattDato());

        return behandling;
    }

    void validerSøknad(Fagsak fagsak, FrisinnSøknad søknad) {
        validerSøknadErForNyPeriode(fagsak, søknad.getSøknadsperiode().getFraOgMed());
        validerIngenÅpneBehandlinger(fagsak);
        validerIngenAvsluttedeBehandlingerIDag(fagsak);
    }

    private Optional<Behandling> finnForrigeAvsluttedeBehandling(Fagsak fagsak) {
        // Den sist avsluttede behandlingen er mest korrekt ift. HISTORISKE inntektsdata som skal videreføres. Derfor
        // brukes sist avsluttede behandling fremfor behandling for sist søknadsperiode.
        // GJELDENDE inntektsdata kommer med selve søknaden.
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
    }

    private void validerSøknadErForNyPeriode(Fagsak fagsak, LocalDate fraOgMed) {
        søknadRepository.hentBehandlingerMedOverlappendeSøknaderIPeriode(fagsak.getId(), fraOgMed, LocalDate.now().plusYears(100)).stream()
            .filter(beh -> !beh.isBehandlingHenlagt())
            .findAny()
            .ifPresent(behandling -> { throw new IllegalStateException("Fant tidligere Frisinn-søknad som overlapper samme periode " +
                "for saksnummer " + fagsak.getSaksnummer() + " fra og med " + fraOgMed); });
    }

    private void validerIngenÅpneBehandlinger(Fagsak fagsak) {
        if (behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).size() > 0) {
            throw new UnsupportedOperationException("Frisinn støtter ikke mottak av søknad for åpne behandlinger, saksnummer = " + fagsak.getSaksnummer());
        }
    }

    private void validerIngenAvsluttedeBehandlingerIDag(Fagsak fagsak) {
        if (flereSøknaderSammeDag) {
            return;
        }
        // For å forhindre at k9-forsendelse gjør flere brevutsendelser samme dag.
        // Økonomi (OS) må ha kjørt utbetaling for returnere riktig etterbetaling til k9-forsendelse.
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandling.isPresent()
            && behandling.get().getAvsluttetDato().toLocalDate().isEqual(LocalDate.now())) {
            throw new UnsupportedOperationException("Frisinn har avsluttet behandling i dag. Ny søknad kan først behandles i morgen, saksnummer = " + fagsak.getSaksnummer());
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

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, null, null, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
