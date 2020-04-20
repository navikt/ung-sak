package no.nav.k9.sak.ytelse.frisinn.mottak;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
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
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;

    SøknadDokumentmottaker() {
        // for CDI proxy
    }

    @Inject
    public SøknadDokumentmottaker(DokumentmottakerFelles dokumentmottakerFelles,
                                  SaksnummerRepository saksnummerRepository,
                                  Behandlingsoppretter behandlingsoppretter,
                                  LagreSøknad søknadOversetter,
                                  FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.soknadOversetter = søknadOversetter;
        this.fagsakTjeneste = fagsakTjeneste;
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
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow();
        var periode = søknad.getSøknadsperiode();
        var eksisterendeBehandlinger = soknadOversetter.finnesSøknadForSammePeriode(fagsak.getId(), periode.getFraOgMed(), periode.getTilOgMed());

        Behandling behandling;
        if (eksisterendeBehandlinger.isEmpty()) {
            behandling = opprettNyBehandling(journalpostId, søknad, fagsak);
        } else {
            Optional<Behandling> åpen = finnÅpenBehandling(eksisterendeBehandlinger);
            if (åpen.isEmpty()) {
                behandling = opprettEndringBehandling(journalpostId, søknad, fagsak);
            } else {
                behandling = åpen.get();
                leggTilÅpenBehandlingOgTilbakefør(søknad, behandling);
            }
        }
        return behandling;
    }

    private void leggTilÅpenBehandlingOgTilbakefør(FrisinnSøknad søknad, Behandling behandling) {
        soknadOversetter.persister(søknad, behandling);
        behandlingsprosessApplikasjonTjeneste.asynkTilbakestillOgÅpneBehandlingForEndringer(behandling.getId(), BehandlingStegType.START_STEG);
    }

    private Behandling opprettEndringBehandling(JournalpostId journalpostId, FrisinnSøknad søknad, Fagsak fagsak) {
        Behandling behandling;
        behandling = behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
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

    private Optional<Behandling> finnÅpenBehandling(List<Behandling> eksisterendeBehandlinger) {
        return eksisterendeBehandlinger.stream().filter(b -> b.erÅpnetForEndring()).findFirst();
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
