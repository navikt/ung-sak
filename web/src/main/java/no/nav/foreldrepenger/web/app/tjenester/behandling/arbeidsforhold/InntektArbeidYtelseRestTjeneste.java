package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

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
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class InntektArbeidYtelseRestTjeneste {

    public static final String INNTEKT_ARBEID_YTELSE_PATH = "/behandling/inntekt-arbeid-ytelse";

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseDtoMapper dtoMapper;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    public InntektArbeidYtelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseRestTjeneste(BehandlingRepository behandlingRepository,
                                           InntektArbeidYtelseDtoMapper dtoMapper,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.dtoMapper = dtoMapper;
    }

    @POST
    @Path(INNTEKT_ARBEID_YTELSE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser", summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."), tags = "inntekt-arbeid-ytelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektArbeidYtelseDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
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
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public InntektArbeidYtelseDto getInntektArbeidYtelser(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return getInntektArbeidYtelserFraBehandling(behandling);
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
