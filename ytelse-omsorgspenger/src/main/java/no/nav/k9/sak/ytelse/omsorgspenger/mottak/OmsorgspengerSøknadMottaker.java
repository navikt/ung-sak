package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerSøknadMottaker implements SøknadMottakTjeneste<OmsorgspengerSøknadInnsending> {

    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private Behandlingsoppretter behandlingsoppretter;

    OmsorgspengerSøknadMottaker() {
        // proxy
    }

    @Inject
    public OmsorgspengerSøknadMottaker(DokumentmottakerFelles dokumentmottakerFelles,
                                       SaksnummerRepository saksnummerRepository,
                                       Behandlingsoppretter behandlingsoppretter,
                                       FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.behandlingsoppretter = behandlingsoppretter;
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
    }

    @Override
    public void mottaSøknad(Saksnummer saksnummer, OmsorgspengerSøknadInnsending søknadInnsending) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknadInnsending);
        // FIXME K9 Legg til logikk for valg av fagsak og behandling type
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow();
        var behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());

        // FIXME K9 Vurder hvordan historikk bør håndteres: Vi trenger ikke kallet under hvis dokumenter fra Joark blir flettet inn ved visning av
        // historikk.
        // dokumentmottakerFelles.opprettHistorikk(behandling, journalPostId);

        // FIXME K9 Persister søknad
        persisterSøknad(behandling, søknadInnsending);

        dokumentmottakerFelles.opprettTaskForÅStarteBehandling(behandling);
    }

    private void persisterSøknad(Behandling behandling, OmsorgspengerSøknadInnsending søknad) {
        throw new UnsupportedOperationException("Ikke implementert ennå for omsorgspenger");
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, LocalDate startDato) {
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, startDato, startDato);
        if (fagsak.isPresent()) {
            return fagsak.get();
        }
        LocalDate yearFom = startDato.withDayOfYear(1);
        LocalDate yearTom = startDato.withMonth(12).withDayOfMonth(31);
        var saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        return opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, yearFom, yearTom);
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        var fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
