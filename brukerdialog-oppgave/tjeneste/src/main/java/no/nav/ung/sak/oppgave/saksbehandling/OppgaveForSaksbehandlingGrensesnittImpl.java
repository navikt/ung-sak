package no.nav.ung.sak.oppgave.saksbehandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndreStatusDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.oppgave.*;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveStatus;
import no.nav.ung.sak.kontrakt.oppgaver.OppgaveType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDTO;
import no.nav.ung.sak.oppgave.typer.varsel.varseltyper.endretperiode.EndretPeriodeOppgaveMapper;
import no.nav.ung.sak.oppgave.typer.varsel.varseltyper.endretsluttdato.EndretSluttdatoOppgaveMapper;
import no.nav.ung.sak.oppgave.typer.varsel.varseltyper.endretstartdato.EndretStartdatoOppgaveMapper;
import no.nav.ung.sak.oppgave.typer.oppgave.inntektsrapportering.InntektsrapporteringOppgaveMapper;
import no.nav.ung.sak.oppgave.typer.varsel.varseltyper.kontrollerregisterinntekt.KontrollerRegisterInntektOppgaveMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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
    public void opprettKontrollerRegisterInntektOppgave(RegisterInntektOppgaveDTO oppgave) {
        oppgaveLivssyklusTjeneste.opprettOppgave(KontrollerRegisterInntektOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettInntektrapporteringOppgave(InntektsrapporteringOppgaveDTO oppgave) {
        oppgaveLivssyklusTjeneste.opprettOppgave(InntektsrapporteringOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettEndretStartdatoOppgave(EndretStartdatoOppgaveDTO oppgave) {
        oppgaveLivssyklusTjeneste.opprettOppgave(EndretStartdatoOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettEndretSluttdatoOppgave(EndretSluttdatoOppgaveDTO oppgave) {
        oppgaveLivssyklusTjeneste.opprettOppgave(EndretSluttdatoOppgaveMapper.map(oppgave, finnAktørId(oppgave.getDeltakerIdent())));
    }

    @Override
    public void opprettEndretPeriodeOppgave(EndretPeriodeOppgaveDTO oppgaveDto) {
        oppgaveLivssyklusTjeneste.opprettOppgave(EndretPeriodeOppgaveMapper.map(oppgaveDto, finnAktørId(oppgaveDto.getDeltakerIdent())));
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
            oppgaveLivssyklusTjeneste.utløpOppgave(oppgave);
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
            oppgaveLivssyklusTjeneste.avbrytOppgave(oppgave);
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
        søkYtelseOppgaver.forEach(oppgaveLivssyklusTjeneste::løsOppgave);
    }

    @Override
    public void endreFrist(String personIdent, UUID eksternReferanse, LocalDateTime frist) {
        AktørId aktørId = finnAktørId(personIdent);
        repository.endreFrist(eksternReferanse, aktørId, frist);
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
        if (oppgave.getData() instanceof InntektsrapporteringOppgavetypeDataDTO data) {
            return data.fraOgMed().equals(fomDato) && data.tilOgMed().equals(tomDato);
        }
        return false;
    }
}

