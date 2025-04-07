package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.etterlysning.kontroll.InntektkontrollEtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.ungdomsprogramperiode.EndretUngdomsprogramperiodeEtterlysningHåndterer;

import java.util.Objects;

@ApplicationScoped
public class EtterlysningProssesseringTjeneste {

    private EtterlysningRepository etterlysningRepository;
    private InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningOppretter;
    private UngOppgaveKlient oppgaveKlient;
    private EndretUngdomsprogramperiodeEtterlysningHåndterer endretUngdomsprogramperiodeEtterlysningHåndterer;

    public EtterlysningProssesseringTjeneste() {
        // CDI
    }

    @Inject
    public EtterlysningProssesseringTjeneste(EtterlysningRepository etterlysningRepository,
                                             InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningOppretter, UngOppgaveKlient oppgaveKlient, EndretUngdomsprogramperiodeEtterlysningHåndterer endretUngdomsprogramperiodeEtterlysningHåndterer) {
        this.etterlysningRepository = etterlysningRepository;
        this.inntektkontrollEtterlysningOppretter = inntektkontrollEtterlysningOppretter;
        this.oppgaveKlient = oppgaveKlient;
        this.endretUngdomsprogramperiodeEtterlysningHåndterer = endretUngdomsprogramperiodeEtterlysningHåndterer;
    }

    public void settTilUtløpt(Long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentUtløpteEtterlysningerSomVenterPåSvar(behandlingId);

        etterlysninger.forEach(e -> {
            oppgaveKlient.oppgaveUtløpt(e.getEksternReferanse());
            e.utløpt();
        });

        etterlysningRepository.lagre(etterlysninger);
    }

    public void settTilAvbrutt(Long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandlingId);

        etterlysninger.forEach(e -> {
            oppgaveKlient.avbrytOppgave(e.getEksternReferanse());
            e.avbryt();
        });

        etterlysningRepository.lagre(etterlysninger);
    }

    public void opprett(Long behandlingId, EtterlysningType etterlysningType) {
        switch (Objects.requireNonNull(etterlysningType)) {
            case UTTALELSE_KONTROLL_INNTEKT ->
                inntektkontrollEtterlysningOppretter.håndterOpprettelse(behandlingId, etterlysningType);
            case UTTALELSE_ENDRET_STARTDATO ->
                endretUngdomsprogramperiodeEtterlysningHåndterer.håndterOpprettelse(behandlingId, etterlysningType);
            case UTTALELSE_ENDRET_SLUTTDATO ->
                endretUngdomsprogramperiodeEtterlysningHåndterer.håndterOpprettelse(behandlingId, etterlysningType);
            default -> throw new IllegalArgumentException("Uhåndtert etterlysningstype: " + etterlysningType);
        }
    }
}
