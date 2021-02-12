package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnValidator;

@Dependent
class SøknadDokumentmottaker {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private SaksnummerRepository saksnummerRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private SøknadOversetter pleiepengerBarnSoknadOversetter;
    private FagsakTjeneste fagsakTjeneste;
    private BehandlingRepository behandlingRepository;

    SøknadDokumentmottaker() {
        // for CDI proxy
    }

    @Inject
    SøknadDokumentmottaker(DokumentmottakerFelles dokumentmottakerFelles,
                           BehandlingRepository behandlingRepository,
                           SaksnummerRepository saksnummerRepository,
                           Behandlingsoppretter behandlingsoppretter,
                           SøknadOversetter pleiepengerBarnSoknadOversetter,
                           FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.behandlingRepository = behandlingRepository;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    Fagsak finnEllerOpprett(FagsakYtelseType fagsakYtelseType, AktørId brukerIdent, AktørId pleietrengendeAktørId, LocalDate startDato, @SuppressWarnings("unused") LocalDate sluttDato) {
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(fagsakYtelseType, brukerIdent, pleietrengendeAktørId, startDato.minusWeeks(25), startDato.plusWeeks(25));
        if (fagsak.isPresent()) {
            return fagsak.get();
        }
        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        return opprettSakFor(saksnummer, brukerIdent, pleietrengendeAktørId, fagsakYtelseType, startDato);
    }

    Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, Søknad søknad) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknad);

        new PleiepengerSyktBarnValidator().forsikreValidert(søknad.getYtelse());

        Behandling behandling = tilknyttBehandling(saksnummer);
        pleiepengerBarnSoknadOversetter.persister(søknad, journalpostId, behandling);

        dokumentmottakerFelles.opprettTaskForÅStarteBehandlingMedNySøknad(behandling, journalpostId);

        return behandling;
    }

    private void validerIngenÅpneBehandlinger(Fagsak fagsak) {
        if (behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).size() > 0) {
            throw new UnsupportedOperationException("PSB støtter ikke mottak av søknad for åpne behandlinger, saksnummer = " + fagsak.getSaksnummer());
        }
    }

    private Behandling tilknyttBehandling(Saksnummer saksnummer) {
        // FIXME K9 Legg til logikk for valg av fagsak
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true).orElseThrow();

        validerIngenÅpneBehandlinger(fagsak);

        // FIXME K9 Legg til logikk for valg av behandlingstype og BehandlingÅrsakType, og BehandlingType.UNNTAKSBEHANDLING (om aktuelt)
        return behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate startDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, saksnummer, startDato, null);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
