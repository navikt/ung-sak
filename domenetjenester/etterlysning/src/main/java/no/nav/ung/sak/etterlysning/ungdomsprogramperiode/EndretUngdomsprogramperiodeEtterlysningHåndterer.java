package no.nav.ung.sak.etterlysning.ungdomsprogramperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.EndretPeriodeOppgaveDTO;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.etterlysning.EtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@Dependent
public class EndretUngdomsprogramperiodeEtterlysningHåndterer implements EtterlysningHåndterer {

    private final EtterlysningRepository etterlysningRepository;
    private final BehandlingRepository behandlingRepository;
    private UngOppgaveKlient ungOppgaveKlient;
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public EndretUngdomsprogramperiodeEtterlysningHåndterer(EtterlysningRepository etterlysningRepository,
                                                            BehandlingRepository behandlingRepository,
                                                            UngOppgaveKlient ungOppgaveKlient, PersoninfoAdapter personinfoAdapter) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.personinfoAdapter = personinfoAdapter;
    }

    public void håndterOpprettelse(long behandlingId, EtterlysningType etterlysningType) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, etterlysningType);
        AktørId aktørId = behandling.getAktørId();
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));

        etterlysninger.forEach(e -> e.vent(getFrist()));
        final var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> new EndretPeriodeOppgaveDTO(
                deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                etterlysning.getPeriode().getFomDato()
            )
        ).toList();

        switch (etterlysningType) {
            case UTTALELSE_ENDRET_STARTDATO -> oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretStartdatoOppgave);
            case UTTALELSE_ENDRET_SLUTTDATO -> oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretSluttdatoOppgave);
            default -> throw new IllegalArgumentException("Ikke støttet etterlysningstype: " + etterlysningType);
        }

        etterlysningRepository.lagre(etterlysninger);
    }
}
