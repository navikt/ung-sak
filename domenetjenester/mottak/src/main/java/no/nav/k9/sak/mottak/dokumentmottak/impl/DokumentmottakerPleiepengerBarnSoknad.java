package no.nav.k9.sak.mottak.dokumentmottak.impl;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.aktør.BrukerTjeneste;
import no.nav.k9.sak.behandlingslager.aktør.NavBruker;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
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
    private BrukerTjeneste brukerTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private PleiepengerBarnSoknadOversetter pleiepengerBarnSoknadOversetter;
    private TpsTjeneste tpsTjeneste;
    private FagsakTjeneste fagsakTjeneste;

    DokumentmottakerPleiepengerBarnSoknad() {
        // for CDI proxy
    }

    @Inject
    public DokumentmottakerPleiepengerBarnSoknad(DokumentmottakerFelles dokumentmottakerFelles,
                                                 BrukerTjeneste brukerTjeneste,
                                                 SaksnummerRepository saksnummerRepository,
                                                 Behandlingsoppretter behandlingsoppretter,
                                                 PleiepengerBarnSoknadOversetter pleiepengerBarnSoknadOversetter,
                                                 TpsTjeneste tpsTjeneste,
                                                 FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.brukerTjeneste = brukerTjeneste;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.pleiepengerBarnSoknadOversetter = pleiepengerBarnSoknadOversetter;
        this.tpsTjeneste = tpsTjeneste;
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

    public Fagsak createNyFagsakFor(FagsakYtelseType fagsakYtelseType, AktørId brukerIdent) {
        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        final Optional<Personinfo> optionalBruker = tpsTjeneste.hentBrukerForAktør(brukerIdent);
        // FIXME K9 Håndter feilsituasjonen når man ikke finner brukeren.
        final Personinfo bruker = optionalBruker.get();
        return opprettSakFor(saksnummer, bruker, fagsakYtelseType);
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, Personinfo bruker, FagsakYtelseType ytelseType) {
        final NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(bruker);
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, navBruker, saksnummer);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
