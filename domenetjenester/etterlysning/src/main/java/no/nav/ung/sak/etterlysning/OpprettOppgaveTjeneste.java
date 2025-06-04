package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.etterlysning.kontroll.InntektkontrollOppgaveOppretter;
import no.nav.ung.sak.etterlysning.sluttdato.EndretSluttdatoOppgaveOppretter;
import no.nav.ung.sak.etterlysning.startdato.EndretStartdatoOppgaveOppretter;

import java.time.Duration;
import java.time.LocalDateTime;

@Dependent
public class OpprettOppgaveTjeneste {

    private final InntektkontrollOppgaveOppretter inntektkontrollOppgaveOppretter;
    private final EndretSluttdatoOppgaveOppretter endretSluttdatoOppgaveOppretter;
    private final EndretStartdatoOppgaveOppretter endretStartdatoOppgaveOppretter;
    private final EtterlysningRepository etterlysningRepository;
    private final BehandlingRepository behandlingRepository;
    private final PersoninfoAdapter personinfoAdapter;
    private final Duration ventePeriode;

    @Inject
    public OpprettOppgaveTjeneste(
        InntektkontrollOppgaveOppretter inntektkontrollOppgaveOppretter,
        EndretSluttdatoOppgaveOppretter endretSluttdatoOppgaveOppretter,
        EndretStartdatoOppgaveOppretter endretStartdatoOppgaveOppretter,
        EtterlysningRepository etterlysningRepository,
        BehandlingRepository behandlingRepository,
        PersoninfoAdapter personinfoAdapter,
        @KonfigVerdi(value = "VENTEFRIST_UTTALELSE", defaultVerdi = "P14D") String ventePeriode
    ) {
        this.inntektkontrollOppgaveOppretter = inntektkontrollOppgaveOppretter;
        this.endretSluttdatoOppgaveOppretter = endretSluttdatoOppgaveOppretter;
        this.endretStartdatoOppgaveOppretter = endretStartdatoOppgaveOppretter;
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.ventePeriode = Duration.parse(ventePeriode);
    }

    public void opprett(long behandlingId, EtterlysningType etterlysningType) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var deltakerIdent = personinfoAdapter.hentIdentForAktørId(behandling.getAktørId()).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, etterlysningType);
        etterlysninger.forEach(e -> e.vent(getFrist()));
        etterlysningRepository.lagre(etterlysninger);
        // REST-kall for å opprette oppgave, gjør dette til slutt
        switch (etterlysningType) {
            case UTTALELSE_KONTROLL_INNTEKT ->
                inntektkontrollOppgaveOppretter.opprettOppgave(behandling, etterlysninger, deltakerIdent);
            case UTTALELSE_ENDRET_STARTDATO ->
                endretStartdatoOppgaveOppretter.opprettOppgave(behandling, etterlysninger, deltakerIdent);
            case UTTALELSE_ENDRET_SLUTTDATO ->
                endretSluttdatoOppgaveOppretter.opprettOppgave(behandling, etterlysninger, deltakerIdent);
            default ->
                throw new IllegalArgumentException("Har ikke implementert oppretting av oppgave for etterlysningstype: " + etterlysningType);
        }
    }

    public LocalDateTime getFrist() {
        return LocalDateTime.now().plus(ventePeriode);
    }

}
