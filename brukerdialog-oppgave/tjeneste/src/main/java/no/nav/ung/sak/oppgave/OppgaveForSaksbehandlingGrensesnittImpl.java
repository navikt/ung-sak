package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndreStatusDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.endretperiode.EndretPeriodeOppgaveMapper;
import no.nav.ung.sak.oppgave.endretsluttdato.EndretSluttdatoOppgaveMapper;
import no.nav.ung.sak.oppgave.endretstartdato.EndretStartdatoOppgaveMapper;
import no.nav.ung.sak.oppgave.inntektsrapportering.InntektsrapporteringOppgaveData;
import no.nav.ung.sak.oppgave.inntektsrapportering.InntektsrapporteringOppgaveMapper;
import no.nav.ung.sak.oppgave.kontrollerregisterinntekt.KontrollerRegisterInntektOppgaveMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Tjeneste for å opprette og administrere brukerdialog-oppgaver.
 * Implementerer BrukerdialogOppgaveService interfacet.
 * <p>
 * Denne tjenesten brukes av etterlysning-modulen og andre moduler
 * som trenger å opprette oppgaver internt i applikasjonen.
 */
@ApplicationScoped
public class OppgaveForSaksbehandlingGrensesnittImpl implements OppgaveForSaksbehandlingGrensesnitt {

    private static final Logger logger = LoggerFactory.getLogger(OppgaveForSaksbehandlingGrensesnittImpl.class);

    private BrukerdialogOppgaveRepository repository;
    private Pdl pdl;

    public OppgaveForSaksbehandlingGrensesnittImpl() {
        // CDI proxy
    }

    @Inject
    public OppgaveForSaksbehandlingGrensesnittImpl(BrukerdialogOppgaveRepository repository,
                                                   Pdl pdl) {
        this.repository = repository;
        this.pdl = pdl;
    }

    @Override
    public void opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO oppgave) {
        repository.persister(KontrollerRegisterInntektOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettInntektrapporteringOppgave(InntektsrapporteringOppgaveDTO oppgave) {
        repository.persister(InntektsrapporteringOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettEndretStartdatoOppgave(EndretStartdatoOppgaveDTO oppgave) {
        repository.persister(EndretStartdatoOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettEndretSluttdatoOppgave(EndretSluttdatoOppgaveDTO oppgave) {
        repository.persister(EndretSluttdatoOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettEndretPeriodeOppgave(EndretPeriodeOppgaveDTO oppgaveDto) {
        repository.persister(EndretPeriodeOppgaveMapper.map(oppgaveDto, finnAktørId(oppgaveDto.getDeltakerIdent())));
    }

    @Override
    public void avbrytOppgave(UUID eksternRef) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(eksternRef)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + eksternRef));

        oppgave.setStatus(OppgaveStatus.AVBRUTT);
        repository.oppdater(oppgave);
    }

    @Override
    public void oppgaveUtløpt(UUID eksternRef) {
        var oppgave = repository.hentOppgaveForOppgavereferanse(eksternRef)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppgave med oppgavereferanse: " + eksternRef));

        oppgave.setStatus(OppgaveStatus.UTLØPT);
        repository.oppdater(oppgave);
    }

    @Override
    public void settOppgaveTilUtløpt(EndreStatusDTO dto) {
        logger.info("Utløper oppgave av type: {} med periode [{} - {}]", dto.getOppgavetype(), dto.getFomDato(), dto.getTomDato());

        AktørId aktørId = finnAktørId(dto.getDeltakerIdent());

        // Hent alle oppgaver for aktøren
        var oppgaver = repository.hentAlleOppgaverForAktør(aktørId);

        // Finn uløst oppgave av riktig type og periode
        var uløstOppgaveISammePeriode = oppgaver.stream()
            .filter(o -> o.getStatus() == OppgaveStatus.ULØST)
            .filter(o -> matcherOppgaveType(o, dto.getOppgavetype()))
            .filter(o -> gjelderSammePeriodeForInntektsrapportering(o, dto.getFomDato(), dto.getTomDato()))
            .findFirst();

        if (uløstOppgaveISammePeriode.isPresent()) {
            var oppgave = uløstOppgaveISammePeriode.get();
            logger.info("Setter oppgave {} til utløpt", oppgave.getOppgavereferanse());
            oppgave.setStatus(OppgaveStatus.UTLØPT);
            repository.oppdater(oppgave);
        } else {
            logger.info("Fant ingen uløst oppgave av type {} for periode [{} - {}]", dto.getOppgavetype(), dto.getFomDato(), dto.getTomDato());
        }
    }

    @Override
    public void settOppgaveTilAvbrutt(EndreStatusDTO dto) {
        logger.info("Avbryter oppgave av type: {} med periode [{} - {}]", dto.getOppgavetype(), dto.getFomDato(), dto.getTomDato());

        AktørId aktørId = finnAktørId(dto.getDeltakerIdent());

        // Hent alle oppgaver for aktøren
        var oppgaver = repository.hentAlleOppgaverForAktør(aktørId);

        // Finn uløst oppgave av riktig type og periode
        var uløstOppgaveISammePeriode = oppgaver.stream()
            .filter(o -> o.getStatus() == OppgaveStatus.ULØST)
            .filter(o -> matcherOppgaveType(o, dto.getOppgavetype()))
            .filter(o -> gjelderSammePeriodeForInntektsrapportering(o, dto.getFomDato(), dto.getTomDato()))
            .findFirst();

        if (uløstOppgaveISammePeriode.isPresent()) {
            var oppgave = uløstOppgaveISammePeriode.get();
            logger.info("Setter oppgave {} til avbrutt", oppgave.getOppgavereferanse());
            oppgave.setStatus(OppgaveStatus.AVBRUTT);
            repository.oppdater(oppgave);
        } else {
            logger.info("Fant ingen uløst oppgave av type {} for periode [{} - {}]", dto.getOppgavetype(), dto.getFomDato(), dto.getTomDato());
        }
    }

    @Override
    public void løsSøkYtelseOppgave(DeltakerDTO deltakerDTO) {
        List<BrukerdialogOppgaveEntitet> søkYtelseOppgaver = repository.hentOppgaveForType(
            OppgaveType.SØK_YTELSE,
            OppgaveStatus.ULØST,
            finnAktørId(deltakerDTO.getDeltakerIdent()));
        if (søkYtelseOppgaver.size() > 1) {
            logger.warn("Fant flere enn én uløst søk-ytelse-oppgave. Antall: {}", søkYtelseOppgaver.size());
        }
        søkYtelseOppgaver.forEach(repository::løsOppgave);
    }

    private AktørId finnAktørId(String deltakerIdent) {
        String aktørIdString = pdl.hentAktørIdForPersonIdent(deltakerIdent, false)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktørId for personIdent"));
        AktørId aktørId = new AktørId(aktørIdString);
        return aktørId;
    }

    private boolean matcherOppgaveType(BrukerdialogOppgaveEntitet oppgave, no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype oppgavetype) {
        if (oppgave.getOppgaveType() == null) {
            return false;
        }
        return oppgave.getOppgaveType().name().equals(oppgavetype.name());
    }

    private boolean gjelderSammePeriodeForInntektsrapportering(BrukerdialogOppgaveEntitet oppgave,
                                                               java.time.LocalDate fomDato,
                                                               java.time.LocalDate tomDato) {
        if (oppgave.getData() instanceof InntektsrapporteringOppgaveData data) {
            return data.getFomDato().equals(fomDato) && data.getTomDato().equals(tomDato);
        }
        return false;
    }
}

