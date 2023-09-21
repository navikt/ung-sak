package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakUtbetalingsgrad;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.uttak.FastsattUttakDto;
import no.nav.k9.sak.kontrakt.uttak.OppgittUttakDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakPeriodeDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakUtbetalingsgradDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrbareUttakAktiviterDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrtUttakDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class UttakRestTjeneste {

    static final String BASE_PATH = "/behandling/uttak";

    public static final String UTTAK_OPPGITT = BASE_PATH + "/oppgitt";
    public static final String UTTAK_FASTSATT = BASE_PATH + "/fastsatt";
    public static final String UTTAK_OVERSTYRT = BASE_PATH + "/overstyrt";
    public static final String UTTAK_OVERSTYRBARE_AKTIVITETER = BASE_PATH + "/overstyrbare-aktiviteter";

    private MapUttak mapUttak;
    private OverstyrUttakRepository overstyrUttakRepository;
    private BehandlingRepository behandlingRepository;

    private UttakTjeneste uttakTjeneste;

    public UttakRestTjeneste() {
        // for proxying
    }

    @Inject
    public UttakRestTjeneste(MapUttak mapOppgittUttak, OverstyrUttakRepository overstyrUttakRepository, BehandlingRepository behandlingRepository, UttakTjeneste uttakTjeneste) {
        this.mapUttak = mapOppgittUttak;
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.behandlingRepository = behandlingRepository;
        this.uttakTjeneste = uttakTjeneste;
    }

    /**
     * Hent oppgitt uttak for angitt behandling.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(UTTAK_OPPGITT)
    @Operation(description = "Hent oppgitt uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Oppgitt uttak fra søknad, null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OppgittUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OppgittUttakDto getOppgittUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();
        return mapUttak.mapOppgittUttak(behandlingId);
    }

    /**
     * Hent fastsatt uttak for angitt behandling.
     */
    @GET
    @Path(UTTAK_FASTSATT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent Fastsatt uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer uttak fastsatt av saksbehandler (fakta avklart før vurdering av uttak), null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FastsattUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public FastsattUttakDto getFastsattUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        UUID behandlingId = behandlingIdDto.getBehandlingUuid();
        return mapUttak.mapFastsattUttak(behandlingId);
    }

    @GET
    @Path(UTTAK_OVERSTYRT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent overstyrt uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer uttak overstyrt av overstyrer, null hvis ikke finnes noe", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OverstyrtUttakDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OverstyrtUttakDto getOverstyrtUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingIdDto) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        LocalDateTimeline<OverstyrtUttakPeriode> overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());
        if (overstyrtUttak.isEmpty()) {
            return null;
        }
        return new OverstyrtUttakDto(overstyrtUttak.stream().map(this::map).toList());
    }

    @POST
    @Path(UTTAK_OVERSTYRBARE_AKTIVITETER)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent overstyrbare aktiviteter for uttak for behandling", tags = "behandling - uttak", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer overstyrbare aktiviteter", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OverstyrbareUttakAktiviterDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public OverstyrbareUttakAktiviterDto hentOverstyrbareAktiviterForUttak(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) OverstyrbareAktiviteterForUttakRequest request) {
        BehandlingUuidDto behandlingIdDto = request.getBehandlingIdDto();
        Periode periode = new Periode(request.getFom(), request.getTom());

        Uttaksplan uttaksplan = uttakTjeneste.hentUttaksplan(behandlingIdDto.getBehandlingUuid(), false);

        Set<OverstyrUttakArbeidsforholdDto> aktiviteter = new LinkedHashSet<>();

        for (Map.Entry<LukketPeriode, UttaksperiodeInfo> entry : uttaksplan.getPerioder().entrySet()) {
            Periode uttakperiode = new Periode(entry.getKey().getFom(), entry.getKey().getTom());
            if (uttakperiode.overlaps(periode)) {
                UttaksperiodeInfo periodeInfo = entry.getValue();
                for (Utbetalingsgrader utbetalingsgrader : periodeInfo.getUtbetalingsgrader()) {
                    aktiviteter.add(map(utbetalingsgrader.getArbeidsforhold()));
                }
            }
        }

        return new OverstyrbareUttakAktiviterDto(new ArrayList<>(aktiviteter));
    }

    public static class OverstyrbareAktiviteterForUttakRequest {

        @Valid
        @NotNull
        private BehandlingUuidDto behandlingIdDto;
        @Valid
        @NotNull
        private LocalDate fom;
        @Valid
        @NotNull
        private LocalDate tom;

        public OverstyrbareAktiviteterForUttakRequest(BehandlingUuidDto behandlingIdDto, LocalDate fom, LocalDate tom) {
            this.behandlingIdDto = behandlingIdDto;
            this.fom = fom;
            this.tom = tom;
        }

        public OverstyrbareAktiviteterForUttakRequest() {
        }

        public BehandlingUuidDto getBehandlingIdDto() {
            return behandlingIdDto;
        }

        public LocalDate getFom() {
            return fom;
        }

        public LocalDate getTom() {
            return tom;
        }

        @AssertTrue(message = "ugyldg periode - fom kan ikke være etter tom")
        boolean gyldigPeriode() {
            return !fom.isAfter(tom);
        }

        @JsonIgnore
        @AbacAttributt("behandlingUuid")
        public UUID getBehandlingUuid() {
            return behandlingIdDto.getBehandlingUuid();
        }
    }


    private OverstyrUttakPeriodeDto map(LocalDateSegment<OverstyrtUttakPeriode> periode) {
        return new OverstyrUttakPeriodeDto(periode.getValue().getId(), new Periode(periode.getFom(), periode.getTom()), periode.getValue().getSøkersUttaksgrad(), map(periode.getValue().getOverstyrtUtbetalingsgrad()), periode.getValue().getBegrunnelse());
    }

    private List<OverstyrUttakUtbetalingsgradDto> map(Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad) {
        return overstyrtUtbetalingsgrad.stream().map(this::map).toList();
    }

    private OverstyrUttakUtbetalingsgradDto map(OverstyrtUttakUtbetalingsgrad overstyrtUtbetalingsgrad) {
        OverstyrUttakArbeidsforholdDto aktivitet = new OverstyrUttakArbeidsforholdDto(
            overstyrtUtbetalingsgrad.getAktivitetType(),
            overstyrtUtbetalingsgrad.getArbeidsgiverId() != null && overstyrtUtbetalingsgrad.getArbeidsgiverId().getOrgnr() != null ? new OrgNummer(overstyrtUtbetalingsgrad.getArbeidsgiverId().getOrgnr()) : null,
            overstyrtUtbetalingsgrad.getArbeidsgiverId() != null ? overstyrtUtbetalingsgrad.getArbeidsgiverId().getAktørId() : null,
            overstyrtUtbetalingsgrad.getInternArbeidsforholdRef());
        return new OverstyrUttakUtbetalingsgradDto(aktivitet, overstyrtUtbetalingsgrad.getUtbetalingsgrad());
    }

    private OverstyrUttakArbeidsforholdDto map(Arbeidsforhold arbeidsforhold) {
        return new OverstyrUttakArbeidsforholdDto(
            UttakArbeidType.fraKode(arbeidsforhold.getType()),
            arbeidsforhold.getOrganisasjonsnummer() != null ? new OrgNummer(arbeidsforhold.getOrganisasjonsnummer()) : null,
            arbeidsforhold.getAktørId() != null ? new AktørId(arbeidsforhold.getAktørId()) : null,
            arbeidsforhold.getArbeidsforholdId() != null ? InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId()) : null
        );
    }


}
