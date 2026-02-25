package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.pdl.Pdl;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.deltaker.DeltakerDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.PeriodeEndringType;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndreFristDto;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.EndreStatusDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.Oppgavetype;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.PeriodeDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.inntektsrapportering.InntektsrapporteringOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.*;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.sak.kontrakt.oppgaver.EndreOppgaveStatusDto;
import no.nav.ung.sak.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.fjernperiode.FjernetPeriodeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.inntektsrapportering.InntektsrapporteringOppgavetypeDataDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.kontrollerregisterinntekt.KontrollerRegisterinntektOppgavetypeDataDto;
import no.nav.ung.sak.oppgave.OppgaveForSaksbehandlingGrensesnitt;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

@Dependent
@ScopedRestIntegration(scopeKey = "ungdomsprogramregister.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-deltakelse-opplyser/.default")
public class UngOppgaveKlient implements OppgaveForSaksbehandlingGrensesnitt {

    private final OidcRestClient restClient;
    private final Pdl pdl;
    private final URI opprettKontrollerRegisterInntektURI;
    private final URI opprettInntektrapporteringURI;
    private final URI utløpForTypeOgPeriodeURI;
    private final URI avbrytForTypeOgPeriodeURI;
    private final URI avbrytURI;
    private final URI utløptURI;
    private final URI opprettEndretSluttdatoURI;
    private final URI opprettEndretStartdatoURI;
    private final URI løsSøkYtelseURI;
    private final URI opprettEndretPeriodeURI;
    private final URI endreFristURI;

    @Inject
    public UngOppgaveKlient(
        OidcRestClient restClient,
        Pdl pdl,
        @KonfigVerdi(value = "ungdomsprogramregister.url", defaultVerdi = "http://ung-deltakelse-opplyser.k9saksbehandling") String url) {
        this.restClient = restClient;
        this.pdl = pdl;
        this.opprettKontrollerRegisterInntektURI = tilUri(url, "oppgave/opprett/kontroll/registerinntekt");
        this.opprettEndretStartdatoURI = tilUri(url, "oppgave/opprett/endret-startdato");
        this.opprettEndretSluttdatoURI = tilUri(url, "oppgave/opprett/endret-sluttdato");
        this.opprettEndretPeriodeURI = tilUri(url, "oppgave/opprett/endret-periode");
        this.opprettInntektrapporteringURI = tilUri(url, "oppgave/opprett/inntektsrapportering");
        this.avbrytURI = tilUri(url, "oppgave/avbryt");
        this.utløptURI = tilUri(url, "oppgave/utlopt");
        this.utløpForTypeOgPeriodeURI = tilUri(url, "oppgave/utlopt/forTypeOgPeriode");
        this.avbrytForTypeOgPeriodeURI = tilUri(url, "oppgave/avbrutt/forTypeOgPeriode");
        this.endreFristURI = tilUri(url, "oppgave/endre/frist");
        this.løsSøkYtelseURI = tilUri(url, "oppgave/los/sokytelse");
    }

    @Override
    public void avbrytOppgave(UUID eksternRef) {
        try {
            restClient.post(avbrytURI, eksternRef);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void opprettOppgave(OpprettOppgaveDto oppgave) {
        try {
            var deltakerIdent = pdl.hentPersonIdentForAktørId(oppgave.aktørId().getId())
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke personident for aktørId: " + oppgave.aktørId()));
            switch (oppgave.oppgavetypeData()) {
                case KontrollerRegisterinntektOppgavetypeDataDto d ->
                    restClient.post(opprettKontrollerRegisterInntektURI, mapTilRegisterInntektOppgaveDTO(deltakerIdent, oppgave.oppgaveReferanse(), oppgave.frist(), d));
                case InntektsrapporteringOppgavetypeDataDto d ->
                    restClient.post(opprettInntektrapporteringURI, mapTilInntektsrapporteringOppgaveDTO(deltakerIdent, oppgave.oppgaveReferanse(), oppgave.frist(), d));
                case EndretStartdatoDataDto d ->
                    restClient.post(opprettEndretStartdatoURI, mapTilEndretStartdatoOppgaveDTO(deltakerIdent, oppgave.oppgaveReferanse(), oppgave.frist(), d));
                case EndretSluttdatoDataDto d ->
                    restClient.post(opprettEndretSluttdatoURI, mapTilEndretSluttdatoOppgaveDTO(deltakerIdent, oppgave.oppgaveReferanse(), oppgave.frist(), d));
                case EndretPeriodeDataDto d ->
                    restClient.post(opprettEndretPeriodeURI, mapTilEndretPeriodeOppgaveDTO(deltakerIdent, oppgave.oppgaveReferanse(), oppgave.frist(), d));
                case FjernetPeriodeDataDto d ->
                    restClient.post(opprettEndretPeriodeURI, mapTilFjernetPeriodeOppgaveDTO(deltakerIdent, oppgave.oppgaveReferanse(), oppgave.frist(), d));
                default -> throw new IllegalArgumentException("Ukjent oppgavetypeData: " + oppgave.oppgavetypeData().getClass().getName());
            }
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void oppgaveUtløpt(UUID eksternRef) {
        try {
            restClient.post(utløptURI, eksternRef);
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void settOppgaveTilUtløpt(EndreOppgaveStatusDto dto) {
        try {
            restClient.post(utløpForTypeOgPeriodeURI, mapTilEndreStatusDTO(dto));
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void settOppgaveTilAvbrutt(EndreOppgaveStatusDto dto) {
        try {
            restClient.post(avbrytForTypeOgPeriodeURI, mapTilEndreStatusDTO(dto));
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void løsSøkYtelseOppgave(String deltakerIdent) {
        try {
            restClient.post(løsSøkYtelseURI, new DeltakerDTO(null, deltakerIdent));
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    @Override
    public void endreFrist(String personIdent, UUID eksternReferanse, LocalDateTime frist) {
        try {
            restClient.post(endreFristURI, new EndreFristDto(eksternReferanse, frist.atZone(ZoneId.systemDefault())));
        } catch (Exception e) {
            throw UngOppgavetjenesteFeil.FACTORY.feilVedKallTilUngOppgaveTjeneste(e).toException();
        }
    }

    // --- Mapping fra ung-sak kontrakt DTOs til deltakelseopplyser DTOs ---

    private static RegisterInntektOppgaveDTO mapTilRegisterInntektOppgaveDTO(String deltakerIdent, UUID ref, LocalDateTime frist, KontrollerRegisterinntektOppgavetypeDataDto d) {
        var arbeidOgFrilans = d.registerinntekt().arbeidOgFrilansInntekter().stream()
            .map(i -> new RegisterInntektArbeidOgFrilansDTO(i.inntekt(), i.arbeidsgiver()))
            .collect(Collectors.toList());
        var ytelse = d.registerinntekt().ytelseInntekter().stream()
            .map(i -> new RegisterInntektYtelseDTO(i.inntekt(), YtelseType.valueOf(i.ytelsetype().name())))
            .collect(Collectors.toList());
        return new RegisterInntektOppgaveDTO(
            deltakerIdent, ref, frist, d.fraOgMed(), d.tilOgMed(),
            new RegisterInntektDTO(arbeidOgFrilans, ytelse), d.gjelderDelerAvMåned());
    }

    private static InntektsrapporteringOppgaveDTO mapTilInntektsrapporteringOppgaveDTO(String deltakerIdent, UUID ref, LocalDateTime frist, InntektsrapporteringOppgavetypeDataDto d) {
        return new InntektsrapporteringOppgaveDTO(deltakerIdent, ref, frist, d.fraOgMed(), d.tilOgMed(), d.gjelderDelerAvMåned());
    }

    private static EndretStartdatoOppgaveDTO mapTilEndretStartdatoOppgaveDTO(String deltakerIdent, UUID ref, LocalDateTime frist, EndretStartdatoDataDto d) {
        return new EndretStartdatoOppgaveDTO(deltakerIdent, ref, frist, d.nyStartdato(), d.forrigeStartdato());
    }

    private static EndretSluttdatoOppgaveDTO mapTilEndretSluttdatoOppgaveDTO(String deltakerIdent, UUID ref, LocalDateTime frist, EndretSluttdatoDataDto d) {
        return new EndretSluttdatoOppgaveDTO(deltakerIdent, ref, frist, d.nySluttdato(), d.forrigeSluttdato());
    }

    private static EndretPeriodeOppgaveDTO mapTilEndretPeriodeOppgaveDTO(String deltakerIdent, UUID ref, LocalDateTime frist, EndretPeriodeDataDto d) {
        return new EndretPeriodeOppgaveDTO(
            deltakerIdent, ref, frist,
            mapPeriode(d.nyPeriode()), mapPeriode(d.forrigePeriode()),
            d.endringer().stream().map(e -> PeriodeEndringType.valueOf(e.name())).collect(Collectors.toSet()));
    }

    private static EndretPeriodeOppgaveDTO mapTilFjernetPeriodeOppgaveDTO(String deltakerIdent, UUID ref, LocalDateTime frist, FjernetPeriodeDataDto d) {
        PeriodeDTO forrigePeriode = new PeriodeDTO(d.forrigeStartdato(), d.forrigeSluttdato());
        return new EndretPeriodeOppgaveDTO(
            deltakerIdent, ref, frist,
            null, forrigePeriode,
            java.util.Set.of(PeriodeEndringType.FJERNET_PERIODE));
    }

    private static EndreStatusDTO mapTilEndreStatusDTO(EndreOppgaveStatusDto dto) {
        return new EndreStatusDTO(
            dto.deltakerIdent(), Oppgavetype.valueOf(dto.oppgavetype().name()),
            dto.fomDato(), dto.tomDato());
    }

    private static PeriodeDTO mapPeriode(no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeDTO periode) {
        if (periode == null) return null;
        return new PeriodeDTO(periode.getFomDato(), periode.getTomDato());
    }

    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for ungdomsprogram.register.url", e);
        }
    }
}
