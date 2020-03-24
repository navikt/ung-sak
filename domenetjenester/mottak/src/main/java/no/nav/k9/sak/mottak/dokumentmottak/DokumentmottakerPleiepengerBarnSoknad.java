package no.nav.k9.sak.mottak.dokumentmottak;

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
import no.nav.k9.sak.mottak.dokumentpersiterer.søknad.psb.PleiepengerBarnSoknadOversetter;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknadValidator;

@Dependent
public class DokumentmottakerPleiepengerBarnSoknad {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private SaksnummerRepository saksnummerRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private PleiepengerBarnSoknadOversetter pleiepengerBarnSoknadOversetter;
    private FagsakTjeneste fagsakTjeneste;

    DokumentmottakerPleiepengerBarnSoknad() {
        // for CDI proxy
    }

    @Inject
    public DokumentmottakerPleiepengerBarnSoknad(DokumentmottakerFelles dokumentmottakerFelles,
                                                 SaksnummerRepository saksnummerRepository,
                                                 Behandlingsoppretter behandlingsoppretter,
                                                 PleiepengerBarnSoknadOversetter pleiepengerBarnSoknadOversetter,
                                                 FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
        this.fagsakTjeneste = fagsakTjeneste;
    }


    public Behandling mottaSoknad(Saksnummer saksnummer, PleiepengerBarnSøknad søknad) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknad);

        new PleiepengerBarnSøknadValidator().forsikreValidert(søknad);

        final Behandling behandling = tilknyttBehandling(saksnummer);
        // FIXME K9 Vurder hvordan historikk bør håndteres: Vi trenger ikke kallet under hvis dokumenter fra Joark blir flettet inn ved visning av historikk.
        // dokumentmottakerFelles.opprettHistorikk(behandling, journalPostId);
        pleiepengerBarnSoknadOversetter.persister(søknad, behandling);

        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        return behandling;
    }

    private Behandling tilknyttBehandling(Saksnummer saksnummer) {
        // FIXME K9 Legg til logikk for valg av fagsak
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow();

        // FIXME K9 Legg til logikk for valg av behandlingstype og BehandlingÅrsakType
        return behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
    }

    public Fagsak finnEllerOpprett(FagsakYtelseType fagsakYtelseType, AktørId brukerIdent, AktørId pleietrengendeAktørId, LocalDate startDato) {
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(fagsakYtelseType, brukerIdent, pleietrengendeAktørId, startDato.minusWeeks(25), startDato.plusWeeks(25));
        if (fagsak.isPresent()) {
            return fagsak.get();
        }
        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        return opprettSakFor(saksnummer, brukerIdent, pleietrengendeAktørId, fagsakYtelseType, startDato);
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate startDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, saksnummer, startDato, null);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
