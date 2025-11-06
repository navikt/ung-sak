package no.nav.ung.domenetjenester.oppgave.gosys;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.k9.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.k9.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.k9.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.ung.domenetjenester.oppgave.behandlendeenhet.BehandlendeEnhet;
import no.nav.ung.domenetjenester.oppgave.behandlendeenhet.BehandlendeEnhetService;
import no.nav.ung.fordel.kodeverdi.GosysKonstanter;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.dokument.FordelBehandlingType;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveFeilmeldinger;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.VirkedagUtil;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

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

    public String opprettEllerHentEksisterendeOppgave(
        OmrådeTema tema,
        BehandlingTema behandlingTema,
        FordelBehandlingType fordelBehandlingType,
        AktørId søkersAktørId,
        JournalpostId journalpostId,
        String beskrivelse,
        GosysKonstanter.Fagsaksystem fagsaksystem,
        GosysKonstanter.OppgaveType oppgaveType
    ) {


        var eksisterendeOppgave = finnEksisterendeOppgave(tema, søkersAktørId, journalpostId, oppgaveType);
        if (eksisterendeOppgave != null) {
            return eksisterendeOppgave.getId().toString();
        }


        BehandlendeEnhet behandlendeEnhet = behandlendeEnhetService.hentBehandlendeEnhet(tema, behandlingTema, søkersAktørId);

        String enhet = behandlendeEnhet.nummer();
        var request = OpprettOppgave.getBuilder()
            .medAktoerId(søkersAktørId.getId())
            .medTildeltEnhetsnr(enhet)
            .medOpprettetAvEnhetsnr(enhet)
            .medAktivDato(LocalDate.now())
            .medFristFerdigstillelse(VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(GosysOppgaveService.FRIST_I_DAGER)))
            .medBeskrivelse(beskrivelse)
            .medTema(tema.getOffisiellKode())
            .medPrioritet(Prioritet.NORM)
            .medBehandlingstema(behandlingTema.getOffisiellKode())
            .medOppgavetype(oppgaveType.getKode())
            .medBehandlesAvApplikasjon(fagsaksystem != null ? fagsaksystem.getKode() : null)
            .medJournalpostId(journalpostId.getVerdi())
            .medBehandlingstype(fordelBehandlingType != null ? fordelBehandlingType.getOffisiellKode() : null);


        var oppgave = klient.opprettetOppgave(request.build());

        return oppgave.getId().toString();
    }

    public Oppgave finnEksisterendeOppgave(OmrådeTema tema, AktørId søkersAktørId, JournalpostId journalpostId, GosysKonstanter.OppgaveType oppgaveType) {
        List<Oppgave> oppgaver = hentEksisterendeOppgaver(tema, søkersAktørId, oppgaveType).stream()
            .filter(it -> it.getJournalpostId().equals(journalpostId.getVerdi()))
            .toList();

        if (!oppgaver.isEmpty()) {
            log.info("Allerede opprettet Gosys journalføringsoppgave for journalpost {}.", journalpostId.getVerdi());
            if (oppgaver.size() != 1) {
                log.warn("{} Gosys journalføringsoppgver opprettet.", oppgaver.size());
                oppgaver.forEach(oppgave -> log.warn("OppgaveId {}", oppgave.getId()));
            }
            return oppgaver.getFirst();
        }
        return null;
    }

    private List<Oppgave> hentEksisterendeOppgaver(OmrådeTema tema, AktørId søkersAktørId, GosysKonstanter.OppgaveType oppgaveType) {
        try {
            return klient.finnAlleOppgaver(
                søkersAktørId.getId(), tema.getOffisiellKode(), List.of(oppgaveType.getKode()));
        } catch (Exception e) {
            throw OppgaveFeilmeldinger.FACTORY.feilVedHentingAvOppgaver(tema, oppgaveType.getKode(), e).toException();
        }
    }

}
