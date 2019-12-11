package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.BrukerTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.k9.soknad.pleiepengerbarn.PleiepengerBarnSoknad;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Optional;

@Dependent
public class DokumentmottakerPleiepengerBarnSoknad {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private BrukerTjeneste brukerTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private PleiepengerBarnSoknadPersister pleiepengerBarnSoknadPersister;
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
                                                 PleiepengerBarnSoknadPersister pleiepengerBarnSoknadPersister,
                                                 TpsTjeneste tpsTjeneste,
                                                 FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.brukerTjeneste = brukerTjeneste;
        this.saksnummerRepository = saksnummerRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.pleiepengerBarnSoknadPersister = pleiepengerBarnSoknadPersister;
        this.tpsTjeneste = tpsTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
    }


    public void mottaSoknad(PleiepengerBarnSoknad soknad) {
        if (soknad == null) {
            throw new IllegalArgumentException("soknad == null");
        }
        final Behandling behandling = tilknyttBehandling(soknad);
        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        // FIXME K9 Vurder hvordan historikk bør håndteres: Vi trenger ikke kallet under hvis dokumenter fra Joark blir flettet inn ved visning av historikk.
        // dokumentmottakerFelles.opprettHistorikk(behandling, journalPostId);
        pleiepengerBarnSoknadPersister.persister(soknad, behandling);
    }

    private Behandling tilknyttBehandling(PleiepengerBarnSoknad soknad) {
        // FIXME K9 Legg til logikk for valg av fagsak
        final Fagsak fagsak = createNyFagsakFor(soknad);

        // FIXME K9 Legg til logikk for valg av behandlingstype og BehandlingÅrsakType
        return behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
    }

    private Fagsak createNyFagsakFor(PleiepengerBarnSoknad soknad) {
        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        final Optional<Personinfo> optionalBruker = tpsTjeneste.hentBrukerForFnr(PersonIdent.fra(soknad.getSoker().getNorskIdentitetsnummer().getVerdi()));
        // FIXME K9 Håndter feilsituasjonen når man ikke finner brukeren.
        final Personinfo bruker = optionalBruker.get();
        final FagsakYtelseType ytelseType = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
        final NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(bruker);
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, navBruker, saksnummer);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
