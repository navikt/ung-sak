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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnValidator;

/**
 * Erstattes av DokumentmottakerPleiepengerSyktBarnSøknad
 */
@Deprecated(forRemoval = true)
@Dependent
class SøknadDokumentmottaker {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private SaksnummerRepository saksnummerRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private SøknadOversetter pleiepengerBarnSoknadOversetter;
    private FagsakTjeneste fagsakTjeneste;
    private SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer;

    SøknadDokumentmottaker() {
        // for CDI proxy
    }

    @Inject
    SøknadDokumentmottaker(DokumentmottakerFelles dokumentmottakerFelles,
                           SaksnummerRepository saksnummerRepository,
                           Behandlingsoppretter behandlingsoppretter,
                           SøknadOversetter pleiepengerBarnSoknadOversetter,
                           SykdomsDokumentVedleggHåndterer sykdomsDokumentVedleggHåndterer,
                           FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.sykdomsDokumentVedleggHåndterer = sykdomsDokumentVedleggHåndterer;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    Fagsak finnEllerOpprett(FagsakYtelseType fagsakYtelseType, AktørId brukerIdent, AktørId pleietrengendeAktørId, LocalDate startDato, LocalDate sluttDato) {
        if (sluttDato == null) {
            sluttDato = startDato;
        }
        if (sluttDato.isAfter(LocalDate.now().plusYears(2))) {
            // Hvis dette skulle bli nødvendig i fremtiden kan denne sjekken fjernes.
            throw new IllegalArgumentException("Fagsak kan ikke være mer enn 2 år inn i fremtiden.");
        }
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(fagsakYtelseType, brukerIdent, pleietrengendeAktørId, null, startDato.minusWeeks(25), sluttDato.plusWeeks(25));
        if (fagsak.isPresent()) {
            return fagsak.get();
        }
        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        return opprettSakFor(saksnummer, brukerIdent, pleietrengendeAktørId, fagsakYtelseType, startDato, sluttDato);
    }

    Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, Søknad søknad) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknad);

        new PleiepengerSyktBarnValidator().forsikreValidert(søknad.getYtelse());

        Behandling behandling = tilknyttBehandling(saksnummer);
        pleiepengerBarnSoknadOversetter.persister(søknad, journalpostId, behandling);

        sykdomsDokumentVedleggHåndterer.leggTilDokumenterSomSkalHåndteresVedlagtSøknaden(behandling, journalpostId,
            behandling.getFagsak().getPleietrengendeAktørId(),
            søknad.getMottattDato().toLocalDateTime());

        dokumentmottakerFelles.opprettTaskForÅStarteBehandlingMedNySøknad(behandling, journalpostId);

        return behandling;
    }

    private Behandling tilknyttBehandling(Saksnummer saksnummer) {
        // FIXME K9 Legg til logikk for valg av fagsak
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true).orElseThrow();

        var sisteBehandling = behandlingsoppretter.hentForrigeBehandling(fagsak);

        if (sisteBehandling.isEmpty()) {
            return behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        } else {
            if (!sisteBehandling.get().erSaksbehandlingAvsluttet()) {
                return sisteBehandling.get();
            }

            // FIXME K9 Legg til logikk for valg av behandlingstype og BehandlingÅrsakType, og BehandlingType.UNNTAKSBEHANDLING (om aktuelt)
            return behandlingsoppretter.opprettRevurdering(sisteBehandling.get(), BehandlingÅrsakType.UDEFINERT);
        }
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate startDato, LocalDate sluttDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, null, saksnummer, startDato, sluttDato);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
