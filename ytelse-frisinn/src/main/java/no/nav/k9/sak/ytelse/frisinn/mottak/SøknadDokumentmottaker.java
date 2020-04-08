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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.søknad.frisinn.FrisinnSøknad;

@Dependent
public class SøknadDokumentmottaker {

    private DokumentmottakerFelles dokumentmottakerFelles;
    private SaksnummerRepository saksnummerRepository;
    private Behandlingsoppretter behandlingsoppretter;
    private LagreSøknad soknadOversetter;
    private FagsakTjeneste fagsakTjeneste;

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

    Behandling mottaSoknad(Saksnummer saksnummer, FrisinnSøknad søknad) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknad);

        final Behandling behandling = tilknyttBehandling(saksnummer);
        soknadOversetter.persister(søknad, behandling);

        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
        return behandling;
    }

    private Behandling tilknyttBehandling(Saksnummer saksnummer) {
        // FIXME K9 Legg til logikk for valg av fagsak
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow();

        // FIXME K9 Legg til logikk for valg av behandlingstype og BehandlingÅrsakType
        return behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
