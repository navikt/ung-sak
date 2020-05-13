package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.OppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.PeriodeDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.SøknadsperiodeOgOppgittOpptjeningDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class InntektArbeidYtelseRestTjeneste {

    public static final String INNTEKT_ARBEID_YTELSE_PATH = "/behandling/inntekt-arbeid-ytelse";
    public static final String OPPGITT_OPPTJEING_PATH = "/behandling/oppgitt-opptjening";

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseDtoMapper dtoMapper;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private UttakRepository uttakRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    public InntektArbeidYtelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseRestTjeneste(BehandlingRepository behandlingRepository,
                                           InntektArbeidYtelseDtoMapper dtoMapper,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           UttakRepository uttakRepository) {
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.dtoMapper = dtoMapper;
        this.uttakRepository = uttakRepository;
    }

    @POST
    @Path(INNTEKT_ARBEID_YTELSE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser", summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."), tags = "inntekt-arbeid-ytelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektArbeidYtelseDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public InntektArbeidYtelseDto getInntektArbeidYtelser(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return getInntektArbeidYtelserFraBehandling(behandling);
    }

    @GET
    @Path(INNTEKT_ARBEID_YTELSE_PATH)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser", summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."), tags = "inntekt-arbeid-ytelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektArbeidYtelseDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public InntektArbeidYtelseDto getInntektArbeidYtelser(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return getInntektArbeidYtelserFraBehandling(behandling);
    }

    @GET
    @Path(OPPGITT_OPPTJEING_PATH)
    @Operation(description = "Hent informasjon om oppgitt opptjening og søknadsperiode", summary = ("Returnerer info om oppgitt opptjening og om hvilken ytelser det blir søkt ytelser for."), tags = "oppgitt-opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer SøknadsperiodeOgOppgittOpptjeningDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SøknadsperiodeOgOppgittOpptjeningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SøknadsperiodeOgOppgittOpptjeningDto getOppgittOpptjening(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        if (grunnlag.isEmpty()) {
            // Fins ikke ennå, returnerer tom dto for legacy kompatibilitet med frontend
            return new SøknadsperiodeOgOppgittOpptjeningDto();
        }

        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = grunnlag.get();
        OppgittOpptjeningDto oppgittOpptjeningDto;

        // viser overstyrt hvis finnes
        if (inntektArbeidYtelseGrunnlag.getOverstyrtOppgittOpptjening().isPresent()) {
            oppgittOpptjeningDto = InntektArbeidYtelseDtoMapper.mapOppgittOpptjening(inntektArbeidYtelseGrunnlag.getOverstyrtOppgittOpptjening());
        } else {
            oppgittOpptjeningDto = InntektArbeidYtelseDtoMapper.mapOppgittOpptjening(inntektArbeidYtelseGrunnlag.getOppgittOpptjening());
        }

        if (oppgittOpptjeningDto != null) {
            Søknadsperioder søknadsperioder = uttakRepository.hentOppgittSøknadsperioder(behandling.getId());
            PeriodeDto periodeFraSøknad = new PeriodeDto(søknadsperioder.getMaksPeriode().getFomDato(), søknadsperioder.getMaksPeriode().getTomDato());
            SøknadsperiodeOgOppgittOpptjeningDto dto = new SøknadsperiodeOgOppgittOpptjeningDto();
            dto.setISøkerPerioden(InntektArbeidYtelseDtoMapper.mapTilPeriode(oppgittOpptjeningDto, periodeFraSøknad));
            dto.setFørSøkerPerioden(InntektArbeidYtelseDtoMapper.mapUtenomPeriode(oppgittOpptjeningDto, periodeFraSøknad));
            dto.setPeriodeFraSøknad(periodeFraSøknad);

            var fastsattUttak = uttakRepository.hentFastsattUttak(behandling.getId());
            boolean søkerYtelseForFrilans = fastsattUttak.getPerioder().stream()
                    .anyMatch(p -> p.getPeriode().overlapper(søknadsperioder.getMaksPeriode()) && p.getAktivitetType() == UttakArbeidType.FRILANSER);

            boolean søkerYtelseForNæring = fastsattUttak.getPerioder().stream()
                    .anyMatch(p -> p.getPeriode().overlapper(søknadsperioder.getMaksPeriode()) && p.getAktivitetType() == UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);

            dto.setSøkerYtelseForNæring(søkerYtelseForNæring);
            dto.setSøkerYtelseForFrilans(søkerYtelseForFrilans);
            return dto;
        }
        return new SøknadsperiodeOgOppgittOpptjeningDto();
    }

    private InntektArbeidYtelseDto getInntektArbeidYtelserFraBehandling(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        if (erSkjæringstidspunktIkkeUtledet(skjæringstidspunkt)) {
            // Tilfelle papirsøknad før registrering
            return new InntektArbeidYtelseDto();
        }
        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        if (grunnlag.isEmpty()) {
            // Fins ikke ennå, returnerer tom dto for legacy kompatibilitet med frontend
            return new InntektArbeidYtelseDto();
        }
        InntektArbeidYtelseGrunnlag iayg = grunnlag.get();

        // finn annen part
        UtledArbeidsforholdParametere param = new UtledArbeidsforholdParametere(
            behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD));

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        return dtoMapper.mapFra(ref, iayg, sakInntektsmeldinger, param);

    }

    private boolean erSkjæringstidspunktIkkeUtledet(Skjæringstidspunkt skjæringstidspunkt) {
        return skjæringstidspunkt == null || !skjæringstidspunkt.getSkjæringstidspunktHvisUtledet().isPresent();
    }
}
