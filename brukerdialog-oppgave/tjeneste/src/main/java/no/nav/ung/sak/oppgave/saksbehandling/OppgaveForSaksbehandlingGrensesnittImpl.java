package no.nav.ung.sak.oppgave.saksbehandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.*;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OppgaveForSaksbehandlingGrensesnittImpl implements OppgaveForSaksbehandlingGrensesnitt {

    private static final Logger logger = LoggerFactory.getLogger(OppgaveForSaksbehandlingGrensesnittImpl.class);

    private BrukerdialogOppgaveRepository repository;
    private OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste;
    private Pdl pdl;
    private boolean isEnabled;

    public OppgaveForSaksbehandlingGrensesnittImpl() {
        // CDI proxy
    }

    @Inject
    public OppgaveForSaksbehandlingGrensesnittImpl(BrukerdialogOppgaveRepository repository,
                                                   OppgaveLivssyklusTjeneste oppgaveLivssyklusTjeneste,
                                                   Pdl pdl,
                                                   @KonfigVerdi(value = "OPPGAVER_I_UNGSAK_ENABLED", defaultVerdi = "true") boolean oppgaverIUngsakEnabled) {
        this.repository = repository;
        this.oppgaveLivssyklusTjeneste = oppgaveLivssyklusTjeneste;
        this.pdl = pdl;
        this.isEnabled = oppgaverIUngsakEnabled;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void opprettOppgave(OpprettOppgaveDto oppgave) {
        BrukerdialogOppgaveEntitet oppgaveEntitet = new BrukerdialogOppgaveEntitet(
            oppgave.oppgaveReferanse(), oppgave.oppgavetypeData().oppgavetype(), oppgave.aktørId(), oppgave.oppgavetypeData(), oppgave.frist());
        oppgaveLivssyklusTjeneste.opprettOppgave(oppgaveEntitet, oppgave.oppgavetypeData());
    }

    @Override
    public void avbrytOppgave(UUID eksternRef) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(eksternRef)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + eksternRef));
        oppgaveLivssyklusTjeneste.avbrytOppgave(oppgave);
    }

    @Override
    public void oppgaveUtløpt(UUID eksternRef) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(eksternRef)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + eksternRef));
        oppgaveLivssyklusTjeneste.utløpOppgave(oppgave);
    }

    @Override
    public void settOppgaveTilUtløpt(EndreOppgaveStatusDto dto) {
        logger.info("Utløper oppgave av type: {} med periode [{} - {}]", dto.oppgavetype(), dto.fomDato(), dto.tomDato());
        var aktørId = finnAktørId(dto.deltakerIdent());
        repository.hentAlleOppgaverForAktør(aktørId).stream()
            .filter(o -> o.getStatus() == OppgaveStatus.ULØST)
            .filter(o -> o.getOppgaveType() == dto.oppgavetype())
            .filter(o -> gjelderSammePeriodeForInntektsrapportering(o, dto))
            .findFirst()
            .ifPresentOrElse(
                oppgave -> {
                    logger.info("Setter oppgave {} til utløpt", oppgave.getOppgavereferanse());
                    oppgaveLivssyklusTjeneste.utløpOppgave(oppgave);
                },
                () -> logger.info("Fant ingen uløst oppgave av type {} for periode [{} - {}]",
                    dto.oppgavetype(), dto.fomDato(), dto.tomDato())
            );
    }

    @Override
    public void settOppgaveTilAvbrutt(EndreOppgaveStatusDto dto) {
        logger.info("Avbryter oppgave av type: {} med periode [{} - {}]", dto.oppgavetype(), dto.fomDato(), dto.tomDato());
        var aktørId = finnAktørId(dto.deltakerIdent());
        repository.hentAlleOppgaverForAktør(aktørId).stream()
            .filter(o -> o.getStatus() == OppgaveStatus.ULØST)
            .filter(o -> o.getOppgaveType() == dto.oppgavetype())
            .filter(o -> gjelderSammePeriodeForInntektsrapportering(o, dto))
            .findFirst()
            .ifPresentOrElse(
                oppgave -> {
                    logger.info("Setter oppgave {} til avbrutt", oppgave.getOppgavereferanse());
                    oppgaveLivssyklusTjeneste.avbrytOppgave(oppgave);
                },
                () -> logger.info("Fant ingen uløst oppgave av type {} for periode [{} - {}]",
                    dto.oppgavetype(), dto.fomDato(), dto.tomDato())
            );
    }

    @Override
    public void løsSøkYtelseOppgave(String deltakerIdent) {
        List<BrukerdialogOppgaveEntitet> søkYtelseOppgaver = repository.hentOppgaveForType(
            OppgaveType.SØK_YTELSE, OppgaveStatus.ULØST, finnAktørId(deltakerIdent));
        if (søkYtelseOppgaver.size() > 1) {
            logger.warn("Fant flere enn én uløst søk-ytelse-oppgave. Antall: {}", søkYtelseOppgaver.size());
        }
        søkYtelseOppgaver.forEach(oppgaveLivssyklusTjeneste::løsOppgave);
    }

    @Override
    public void endreFrist(String personIdent, UUID eksternReferanse, LocalDateTime frist) {
        repository.endreFrist(eksternReferanse, finnAktørId(personIdent), frist);
    }

    private AktørId finnAktørId(String deltakerIdent) {
        String aktørIdString = pdl.hentAktørIdForPersonIdent(deltakerIdent, false)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktørId for personIdent"));
        return new AktørId(aktørIdString);
    }

    private boolean gjelderSammePeriodeForInntektsrapportering(BrukerdialogOppgaveEntitet oppgave,
                                                               EndreOppgaveStatusDto dto) {
        if (oppgave.getData() instanceof InntektsrapporteringOppgavetypeDataDto data) {
            return data.fraOgMed().equals(dto.fomDato()) && data.tilOgMed().equals(dto.tomDato());
        }
        return false;
    }
}
