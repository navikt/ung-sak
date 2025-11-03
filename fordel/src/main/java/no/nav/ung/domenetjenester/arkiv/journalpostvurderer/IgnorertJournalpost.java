package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.Journalstatus;
import no.nav.k9.felles.integrasjon.saf.Tema;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.domenetjenester.arkiv.JournalføringHendelsetype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.StrukturertJournalpost.GODKJENTE_KODER;
import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.håndtert;
import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.ikkeHåndtert;

@Dependent
public class IgnorertJournalpost implements Journalpostvurderer {

    private static final Logger log = LoggerFactory.getLogger(IgnorertJournalpost.class);

    private static final List<Tema> RELEVANTE_TEMAER = List.of(Tema.UNG);
    private final boolean enableHåndterAndreJournalposter;

    @Inject
    public IgnorertJournalpost(@KonfigVerdi(value = "ENABLE_HANDTER_ANDRE_JOURNALPOSTER", defaultVerdi = "false") boolean enableHåndterAndreJournalposter) {
        this.enableHåndterAndreJournalposter = enableHåndterAndreJournalposter;
    }

    @Override
    public VurdertJournalpost gjørVurdering(Vurderingsgrunnlag vurderingsgrunnlag) {
        var melding = vurderingsgrunnlag.melding();
        var journalpostInfo = vurderingsgrunnlag.journalpostInfo();
        Tema temaPåMelding = no.nav.k9.felles.integrasjon.saf.Tema.valueOf(melding.getTema().getKode());
        Tema temaPåJournalpostInfo = journalpostInfo.getTema();

        if (!RELEVANTE_TEMAER.contains(temaPåMelding) || !RELEVANTE_TEMAER.contains(temaPåJournalpostInfo)) {
            log.info("Ignorerer dokument som ikke er på forventet tema. Tema=" + temaPåJournalpostInfo);
            return håndtert();
        }

        // Håndterer journalposthendelse med type som ikke matcher med status i arkiv.
        Journalstatus journalstatus = journalpostInfo.getJournalstatus();
        Optional<JournalføringHendelsetype> hendelsetype = vurderingsgrunnlag.melding().getJournalføringHendelsetype();

        if (hendelseErMottattMenStatusErJournalført(hendelsetype, journalstatus)) {
            log.info("HendelseType på journalpost ({}) virker å være MOTTATT, men journalpost har status: {} i arkiv. Ignorerer", vurderingsgrunnlag.journalpostInfo(), journalstatus);
            return håndtert();
        }

        if (enableHåndterAndreJournalposter) {
            return ikkeHåndtert();
        }

        if (ignorer(vurderingsgrunnlag)) {
            // Feiler her i stedet for å ignorere, slik at vi kan se hvilke brevkoder som ikke er håndtert.
            throw new IllegalArgumentException("Fikk brevkode som ikke kunne håndteres: " + journalpostInfo.getBrevkode());
        }

        return ikkeHåndtert();
    }

    private boolean hendelseErMottattMenStatusErJournalført(Optional<JournalføringHendelsetype> hendelsetype, Journalstatus journalstatus) {
        boolean hendelsetypeErMottatt = hendelsetype.isPresent() && hendelsetype.get() == JournalføringHendelsetype.MOTTATT;
        boolean erJournalførtEllerFerdigstilt = Journalstatus.JOURNALFOERT.equals(journalstatus ) || Journalstatus.FERDIGSTILT.equals(journalstatus);
        return hendelsetypeErMottatt && erJournalførtEllerFerdigstilt;
    }

    private boolean ignorer(Vurderingsgrunnlag vurderingsgrunnlag) {
        var brevkode = vurderingsgrunnlag.journalpostInfo().getBrevkode();
        return brevkode == null || !GODKJENTE_KODER.contains(brevkode);
    }
}
