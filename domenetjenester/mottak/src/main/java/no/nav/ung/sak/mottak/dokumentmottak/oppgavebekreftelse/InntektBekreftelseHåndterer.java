package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningEntitet;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;

@Dependent
@OppgaveTypeRef(Bekreftelse.Type.UNG_AVVIK_REGISTERINNTEKT)
public class InntektBekreftelseHåndterer implements BekreftelseHåndterer {


    private EtterlysningRepository etterlysningRepository;

    @Inject
    public InntektBekreftelseHåndterer(EtterlysningRepository etterlysningRepository) {
        this.etterlysningRepository = etterlysningRepository;
    }

    @Override
    public void håndter(OppgaveBekreftelseInnhold bekreftelse) {
        // hent tilhørende etterlysning og marker den som løst
        InntektBekreftelse b = bekreftelse.oppgaveBekreftelse().getBekreftelse();
        UUID oppgaveId = b.getOppgaveId();
        EtterlysningEntitet etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(oppgaveId);
        etterlysning.mottattSvar();
        etterlysningRepository.lagre(etterlysning);

        // opprett uttalelse hvis finnes
        // lagre grunnlag


        // ta behandling av vent
    }
}
