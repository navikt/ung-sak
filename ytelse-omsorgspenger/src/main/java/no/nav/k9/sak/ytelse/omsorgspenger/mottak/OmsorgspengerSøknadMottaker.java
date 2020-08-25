package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentmottakerFelles;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerSøknadMottaker implements SøknadMottakTjeneste<OmsorgspengerSøknadInnsending> {

    private static final int CUT_OFF_OMP = 2020;
    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingRepository behandlingRepository;

    OmsorgspengerSøknadMottaker() {
        // proxy
    }

    @Inject
    public OmsorgspengerSøknadMottaker(DokumentmottakerFelles dokumentmottakerFelles,
                                       BehandlingRepository behandlingRepository,
                                       SaksnummerRepository saksnummerRepository,
                                       Behandlingsoppretter behandlingsoppretter,
                                       FagsakTjeneste fagsakTjeneste) {
        this.dokumentmottakerFelles = dokumentmottakerFelles;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsoppretter = behandlingsoppretter;
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
    }

    @Override
    public Behandling mottaSøknad(Saksnummer saksnummer, JournalpostId journalpostId, OmsorgspengerSøknadInnsending søknadInnsending) {
        Objects.requireNonNull(saksnummer);
        Objects.requireNonNull(søknadInnsending);

        // FIXME K9 Legg til logikk for valg av fagsak og behandling type
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow();
        validerIngenÅpneBehandlinger(fagsak);

        var behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());

        // FIXME K9 Persister søknad
        persisterSøknad(behandling, søknadInnsending);

        dokumentmottakerFelles.opprettTaskForÅStarteBehandlingMedNySøknad(behandling, journalpostId);

        return behandling;
    }

    private void validerIngenÅpneBehandlinger(Fagsak fagsak) {
        if (behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).size() > 0) {
            throw new UnsupportedOperationException("Omsorgspenger støtter ikke mottak av søknad for åpne behandlinger, saksnummer = " + fagsak.getSaksnummer());
        }
    }

    @SuppressWarnings("unused")
    private void persisterSøknad(Behandling behandling, OmsorgspengerSøknadInnsending søknad) {
        throw new UnsupportedOperationException("Ikke implementert ennå for omsorgspenger");
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, LocalDate startDato, LocalDate sluttDato) {
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, startDato, sluttDato);
        if (fagsak.isPresent()) {
            return fagsak.get();
        }

        var saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());

        LocalDate idag = LocalDate.now();
        var detteÅret = DatoIntervallEntitet.fraOgMedTilOgMed(idag.withDayOfYear(1), idag.withMonth(12).withDayOfMonth(31));
        var forrigeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusYears(1).withDayOfYear(1), idag.minusYears(1).withMonth(12).withDayOfMonth(31));
        var angittPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);

        for (var p : Arrays.asList(detteÅret, forrigeÅr)) {
            if (p.overlapper(angittPeriode)) {
                if (p.getFomDato().getYear() >= CUT_OFF_OMP) {
                    // ta utgangspunkt i året i år først, sjekk deretter fjoråret. Men ikke tillatt 2019 eller tidligere her
                    return opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, p.getFomDato(), p.getTomDato());
                }
            }
        }

        throw new IllegalArgumentException("Kan ikke opprette " + ytelseType + " sak for periode: " + angittPeriode);
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        var fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
