package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.etterlysning.kontroll.InntektkontrollEtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.sluttdato.EndretSluttdatoEtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.startdato.EndretStartdatoEtterlysningHåndterer;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class EtterlysningProssesseringTjeneste {

    private EtterlysningRepository etterlysningRepository;
    private InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningHåndterer;
    private EndretStartdatoEtterlysningHåndterer endretStartdatoEtterlysningHåndterer;
    private EndretSluttdatoEtterlysningHåndterer endretSluttdatoEtterlysningHåndterer;

    private UngOppgaveKlient oppgaveKlient;

    public EtterlysningProssesseringTjeneste() {
        // CDI
    }

    @Inject
    public EtterlysningProssesseringTjeneste(EtterlysningRepository etterlysningRepository,
                                             InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningHåndterer,
                                             UngOppgaveKlient oppgaveKlient,
                                             EndretStartdatoEtterlysningHåndterer endretStartdatoEtterlysningHåndterer,
                                             EndretSluttdatoEtterlysningHåndterer endretSluttdatoEtterlysningHåndterer) {
        this.etterlysningRepository = etterlysningRepository;
        this.inntektkontrollEtterlysningHåndterer = inntektkontrollEtterlysningHåndterer;
        this.oppgaveKlient = oppgaveKlient;
        this.endretStartdatoEtterlysningHåndterer = endretStartdatoEtterlysningHåndterer;
        this.endretSluttdatoEtterlysningHåndterer = endretSluttdatoEtterlysningHåndterer;
    }

    public void settTilUtløpt(Long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentUtløpteEtterlysningerSomVenterPåSvar(behandlingId);

        settEttelysningerUtløpt(etterlysninger);
    }

    public void settEttelysningerUtløpt(List<Etterlysning> etterlysninger) {
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
                inntektkontrollEtterlysningHåndterer.håndterOpprettelse(behandlingId, etterlysningType);
            case UTTALELSE_ENDRET_STARTDATO ->
                endretStartdatoEtterlysningHåndterer.håndterOpprettelse(behandlingId, etterlysningType);
            case UTTALELSE_ENDRET_SLUTTDATO ->
                endretSluttdatoEtterlysningHåndterer.håndterOpprettelse(behandlingId, etterlysningType);
            default -> throw new IllegalArgumentException("Uhåndtert etterlysningstype: " + etterlysningType);
        }
    }
}
