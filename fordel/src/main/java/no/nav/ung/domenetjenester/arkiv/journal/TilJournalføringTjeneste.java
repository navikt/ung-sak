package no.nav.ung.domenetjenester.arkiv.journal;


import static no.nav.ung.domenetjenester.arkiv.JournalpostProjectionBuilder.byggJournalpostResponseProjection;

import java.util.Optional;

import no.nav.ung.kodeverk.Fagsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.Journalstatus;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.ung.domenetjenester.arkiv.dok.DokTjeneste;
import no.nav.ung.domenetjenester.arkiv.dok.model.Bruker;
import no.nav.ung.domenetjenester.arkiv.dok.model.BrukerIdType;
import no.nav.ung.domenetjenester.arkiv.dok.model.OppdaterJournalpostRequest;
import no.nav.ung.domenetjenester.arkiv.dok.model.Sak;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.sak.typer.JournalpostId;


@Dependent
public class TilJournalføringTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(TilJournalføringTjeneste.class);
    private DokTjeneste dokTjeneste;
    private SafTjeneste safTjeneste;

    @Inject
    public TilJournalføringTjeneste(DokTjeneste dokTjeneste, SafTjeneste safTjeneste) {
        this.dokTjeneste = dokTjeneste;
        this.safTjeneste = safTjeneste;
    }

    public boolean tilJournalføring(JournalpostId journalpostId, Optional<String> sakId, OmrådeTema tema, String aktørId) {
        LOG.info("Forsøker ferdigstillelse av journalpostId={}, mot sak={}", journalpostId, sakId);
        if (aktørId.isEmpty()) {
            throw new IllegalArgumentException("AktørId er påkrevd");
        }

        oppdaterJournalpost(sakId, journalpostId, aktørId, tema);

        dokTjeneste.ferdigstillJournalpost(journalpostId);
        return true;
    }

    public boolean erAlleredeJournalført(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());

        var journalpost = safTjeneste.hentJournalpostInfo(query, byggJournalpostResponseProjection());
        if (!Journalstatus.MOTTATT.equals(journalpost.getJournalstatus())) {
            LOG.info("Behandler journalpost som ikke har status MOTTATT status={} fagsystem={} saksnummer={}",
                journalpost.getJournalstatus(),
                (journalpost.getSak() != null) ? journalpost.getSak().getFagsaksystem() : "",
                (journalpost.getSak() != null) ? journalpost.getSak().getFagsakId() : "");
        }
        if (Journalstatus.FEILREGISTRERT.equals(journalpost.getJournalstatus())
            || Journalstatus.UTGAAR.equals(journalpost.getJournalstatus())
            || Journalstatus.FERDIGSTILT.equals(journalpost.getJournalstatus())) {
            LOG.warn(String.format("Stopper behandling av journalpost %s med status %s", journalpostId.getVerdi(), journalpost.getJournalstatus()));
            return true;
        }
        if (Journalstatus.JOURNALFOERT.equals(journalpost.getJournalstatus())) {
            return true;
        }
        return false;
    }


    private void oppdaterJournalpost(Optional<String> sakId, JournalpostId journalpostId, String aktørId, OmrådeTema tema) {
        var oppdaterJournalpostRequest = new OppdaterJournalpostRequest();
        var bruker = new Bruker(aktørId, BrukerIdType.AKTOERID);
        oppdaterJournalpostRequest.setBruker(bruker);
        oppdaterJournalpostRequest.setTema(tema.getOffisiellKode());

        Sak sak = new Sak(Fagsystem.UNG_SAK.getOffisiellKode(), sakId.orElseThrow(), no.nav.ung.domenetjenester.arkiv.dok.model.Sakstype.FAGSAK);
        oppdaterJournalpostRequest.setSak(sak);

        dokTjeneste.oppdaterJournalpost(journalpostId, oppdaterJournalpostRequest);
    }
}
