package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;


import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.domenetjenester.arkiv.JournalføringHendelsetype;
import no.nav.ung.domenetjenester.oppgave.gosys.OpprettOppgaveTask;
import no.nav.ung.fordel.kodeverdi.GosysKonstanter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.håndtert;
import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.ikkeHåndtert;

@ApplicationScoped
public class UhåndtertJournalpost implements Journalpostvurderer {

    private static final Logger log = LoggerFactory.getLogger(UhåndtertJournalpost.class);

    private static final Set<JournalføringHendelsetype> relevanteHendelsetyper = Set.of(
            JournalføringHendelsetype.MOTTATT,
            JournalføringHendelsetype.TEMA_ENDRET,
            JournalføringHendelsetype.ENDELING_JOURNALFØRT
    );

    @Override
    public Set<JournalføringHendelsetype> relevanteHendelsetyper() {
        return relevanteHendelsetyper;
    }

    @Override
    public VurdertJournalpost gjørVurdering(Vurderingsgrunnlag vurderingsgrunnlag) {
        if (vurderingsgrunnlag.erEndeligJournalført()) {
            return vurderEndeligJournalørt(vurderingsgrunnlag);
        } else {
            return vurderAnnetEnnEndeligJournalført(vurderingsgrunnlag);
        }
    }

    private VurdertJournalpost vurderEndeligJournalørt(Vurderingsgrunnlag vurderingsgrunnlag) {
        if (!vurderingsgrunnlag.tilhørerUng()) {
            log.info(vurderingsgrunnlag.logMelding("Endelig journalført mot != ung"));
        }
        return ikkeHåndtert();
    }

    private VurdertJournalpost vurderAnnetEnnEndeligJournalført(Vurderingsgrunnlag vurderingsgrunnlag) {
        var melding = vurderingsgrunnlag.melding();
        var journalpostInfo = vurderingsgrunnlag.journalpostInfo();
        if (!journalpostInfo.harBrevkode()) {
            melding.setBeskrivelse("Må manuelt journalføres siden det mangler data som er påkrevd for automatisk journalføring. Mangler brevkode");
        }
        melding.setOppgaveType(GosysKonstanter.OppgaveType.JOURNALFØRING);
        return håndtert(melding.nesteSteg(OpprettOppgaveTask.TASKTYPE));
    }

}
