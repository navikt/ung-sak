package no.nav.ung.domenetjenester.oppgave.gosys;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.k9.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.k9.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.ung.domenetjenester.oppgave.behandlendeenhet.BehandlendeEnhet;
import no.nav.ung.domenetjenester.oppgave.behandlendeenhet.BehandlendeEnhetService;
import no.nav.ung.fordel.kodeverdi.FordelBehandlingType;
import no.nav.ung.fordel.kodeverdi.GosysKonstanter;
import no.nav.ung.fordel.kodeverdi.OmrådeTema;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.VirkedagUtil;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;

@Dependent
public class GosysOppgaveService {

    private static final Logger log = LoggerFactory.getLogger(GosysOppgaveService.class);
    private static final Integer FRIST_I_DAGER = 3;

    private OppgaveRestKlient klient;
    private BehandlendeEnhetService behandlendeEnhetService;

    GosysOppgaveService() {
        // CDI
    }

    @Inject
    GosysOppgaveService(OppgaveRestKlient klient,
                        BehandlendeEnhetService behandlendeEnhetService) {
        this.klient = klient;
        this.behandlendeEnhetService = behandlendeEnhetService;
    }

    public String opprettOppgave(OmrådeTema områdeTema,
                                 BehandlingTema behandlingTema,
                                 FordelBehandlingType fordelBehandlingType,
                                 AktørId søkersAktørId,
                                 JournalpostId journalpostId,
                                 String beskrivelse,
                                 GosysKonstanter.Fagsaksystem fagsaksystem,
                                 GosysKonstanter.OppgaveType oppgaveType
    ) {


        var eksisterendeOppgave = hentEksisterendeOppgave(områdeTema, søkersAktørId, journalpostId);
        if (eksisterendeOppgave != null) {
            return eksisterendeOppgave;
        }


        BehandlendeEnhet behandlendeEnhet = behandlendeEnhetService.hentBehandlendeEnhet(områdeTema, behandlingTema, søkersAktørId);

        var request = createRestRequestBuilder(
            søkersAktørId,
            behandlendeEnhet.nummer,
            beskrivelse,
            Prioritet.NORM,
            FRIST_I_DAGER,
            områdeTema.getOffisiellKode()
        ).medBehandlingstema(behandlingTema.getOffisiellKode())
            .medOppgavetype(oppgaveType.getKode())
            .medBehandlesAvApplikasjon(fagsaksystem.getKode())
            .medJournalpostId(journalpostId.getVerdi())
            .medBehandlingstype(fordelBehandlingType != null ? fordelBehandlingType.getOffisiellKode() : null);


        var oppgave = klient.opprettetOppgave(request.build());

        return oppgave.getId().toString();
    }

    private OpprettOppgave.Builder createRestRequestBuilder(AktørId aktørId, String enhet, String beskrivelse, Prioritet prioritet, int fristDager, String tema) {
        return OpprettOppgave.getBuilder()
            .medAktoerId(aktørId.getId())
            .medTildeltEnhetsnr(enhet)
            .medOpprettetAvEnhetsnr(enhet)
            .medAktivDato(LocalDate.now())
            .medFristFerdigstillelse(VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(fristDager)))
            .medBeskrivelse(beskrivelse)
            .medTema(tema)
            .medPrioritet(prioritet);
    }

    private String hentEksisterendeOppgave(OmrådeTema områdeTema, AktørId søkersAktørId, JournalpostId journalpostId) {
        return hentEksisterendeOppgave(områdeTema, søkersAktørId, journalpostId, false);
    }

    public String hentEksisterendeOppgave(OmrådeTema områdeTema, AktørId søkersAktørId, JournalpostId journalpostId, boolean kunÅpneOppgaver) {
        // TODO: Utvid klient med mulighet for å finne oppgaver med gitt journalpostid
        return null;
    }

}
