package no.nav.ung.sak.web.app.oppgave;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.feil.Feil;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.produksjonsstyring.OppgaveIdDto;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.ung.sak.typer.Saksnummer;

public class OppgaveRedirectData {

    private static final Logger LOGGER = LoggerFactory.getLogger(OppgaveRedirectData.class);

    private Saksnummer saksnummer;
    private Long behandlingId;
    private String feilmelding;

    static OppgaveRedirectData hent(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository, FagsakRepository fagsakRepository,
                                    OppgaveIdDto oppgaveId, SaksnummerDto saksnummerDto) {
        if (oppgaveId == null && saksnummerDto == null) {
            return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.sakKanIkkeÅpnesDaReferanseMangler()));
        } else if (oppgaveId == null) {
            Saksnummer saksnummer = saksnummerDto.getVerdi();
            return hentForFagsak(fagsakRepository,saksnummer);
        } else if (saksnummerDto == null) {
            return hentForOppgave(oppgaveBehandlingKoblingRepository, fagsakRepository, oppgaveId.getVerdi());
        }

        Saksnummer saksnummer = saksnummerDto.getVerdi();
        Optional<Fagsak> sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (sak.isEmpty()) {
            return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.detFinnesIngenFagsak(saksnummer.getVerdi())));
        }

        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId.getVerdi());
        if (oppgave.isPresent()) {
            Behandling behandling = oppgave.get().getBehandling();
            Fagsak fagsak = fagsakRepository.finnEksaktFagsak(behandling.getFagsakId());
            if (fagsak.getSaksnummer() != null && fagsak.getSaksnummer().equals(saksnummer)) {
                return OppgaveRedirectData.medSaksnummerOgBehandlingId(fagsak.getSaksnummer(), behandling.getId());
            }
            return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.oppgaveErIkkeRegistrertPåSak(oppgaveId.getVerdi(), saksnummer.getVerdi())));
        }

        return OppgaveRedirectData.medSaksnummer(saksnummer);
    }

    private static OppgaveRedirectData hentForFagsak(FagsakRepository fagsakRepository, Saksnummer saksnummer) {
        Optional<Fagsak> sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (sak.isPresent()) {
            return OppgaveRedirectData.medSaksnummer(saksnummer);
        }
        return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.detFinnesIngenFagsak(saksnummer.getVerdi())));
    }

    private static OppgaveRedirectData hentForOppgave(OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                                      FagsakRepository fagsakRepository, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        if (oppgave.isPresent()) {
            Behandling behandling = oppgave.get().getBehandling();
            Fagsak fagsak = fagsakRepository.finnEksaktFagsak(behandling.getFagsakId());
            return OppgaveRedirectData.medSaksnummerOgBehandlingId(fagsak.getSaksnummer(), behandling.getId());
        }
        return OppgaveRedirectData.medFeilmelding(logg(OppgaveRedirectServletFeil.FACTORY.detFinnesIngenOppgaveMedDenneReferansen(oppgaveId)));
    }

    private static OppgaveRedirectData medSaksnummerOgBehandlingId(Saksnummer saksnummer, Long behandlingId) {
        OppgaveRedirectData data = new OppgaveRedirectData();
        data.saksnummer = saksnummer;
        data.behandlingId = behandlingId;
        return data;
    }

    private static OppgaveRedirectData medSaksnummer(Saksnummer saksnummer) {
        OppgaveRedirectData data = new OppgaveRedirectData();
        data.saksnummer = saksnummer;
        return data;
    }

    static OppgaveRedirectData medFeilmelding(String feilmelding) {
        OppgaveRedirectData data = new OppgaveRedirectData();
        try {
            data.feilmelding = URLEncoder.encode(feilmelding, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            data.feilmelding = logg(OppgaveRedirectServletFeil.FACTORY.kunneIkkeEncodeFeilmelding(feilmelding, e));
        }
        return data;
    }

    static String logg(Feil feil) {
        feil.log(LOGGER);
        return feil.getFeilmelding();
    }

    Saksnummer getSaksnummer() {
        return saksnummer;
    }

    Long getBehandlingId() {
        return behandlingId;
    }

    String getFeilmelding() {
        return feilmelding;
    }

    boolean harBehandlingId() {
        return behandlingId != null;
    }

    boolean harFeilmelding() {
        return feilmelding != null;
    }
}
