package no.nav.ung.domenetjenester.arkiv.journalpostvurderer;


import static no.nav.ung.domenetjenester.arkiv.journalpostvurderer.VurdertJournalpost.ikkeHåndtert;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.domenetjenester.arkiv.JournalføringHendelsetype;

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
        if (!vurderingsgrunnlag.tilhørerK9()) {
            log.info(vurderingsgrunnlag.logMelding("Endelig journalført mot != K9"));
        }
        return ikkeHåndtert();
    }

    private VurdertJournalpost vurderAnnetEnnEndeligJournalført(Vurderingsgrunnlag vurderingsgrunnlag) {
        // TODO: SKal vi journalposter som ikkje er endelig journalført?
//        var melding = vurderingsgrunnlag.melding();
//        var journalpostInfo = vurderingsgrunnlag.journalpostInfo();
//        if (!journalpostInfo.harBrevkode()) {
//            melding.setBeskrivelse("Må manuelt journalføres siden det mangler data som er påkrevd for automatisk journalføring. Mangler brevkode");
//        }
//        melding.setOppgaveType(GosysKonstanter.OppgaveType.JOURNALFØRING);
//        return håndtert(melding.nesteSteg(OpprettOppgaveTask.TASKTYPE));
        return ikkeHåndtert();
    }

}
