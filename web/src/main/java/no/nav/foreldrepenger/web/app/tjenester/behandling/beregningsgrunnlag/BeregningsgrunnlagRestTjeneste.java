package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.Optional;

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
import no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta.BeregningsgrunnlagDtoTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.BeregningsgrunnlagDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

/**
 * Beregningsgrunnlag knyttet til en behandling.
 */
@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class BeregningsgrunnlagRestTjeneste {

    static public final String PATH = "/behandling/beregningsgrunnlag";
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagDtoTjeneste beregningsgrunnlagDtoTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private BeregningsgrunnlagInputProvider inputTjenesteProvider;

    public BeregningsgrunnlagRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsgrunnlagRestTjeneste(BehandlingRepository behandlingRepository,
                                          OpptjeningRepository opptjeningRepository,
                                          BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                          BeregningsgrunnlagDtoTjeneste beregningsgrunnlagDtoTjeneste,
                                          InntektArbeidYtelseTjeneste iayTjeneste) {
        this.opptjeningRepository = opptjeningRepository;
        this.inputTjenesteProvider = inputTjenesteProvider;
        this.beregningsgrunnlagDtoTjeneste = beregningsgrunnlagDtoTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path(PATH)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsgrunnlagDto hentBeregningsgrunnlag(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingId) {
        Long id = behandlingId.getBehandlingId();
        var behandling = id != null
            ? behandlingRepository.hentBehandling(id)
            : behandlingRepository.hentBehandling(behandlingId.getBehandlingUuid());
        final var opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
        if (opptjening.isEmpty()) {
            return null;
        }
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = iayTjeneste.finnGrunnlag(id);
        return iayGrunnlagOpt.flatMap(iayGrunnlag -> {
            var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandling, iayGrunnlag);
            if (input.isPresent()) {
                return beregningsgrunnlagDtoTjeneste.lagBeregningsgrunnlagDto(input.get());
            } else {
                return Optional.empty();
            }
        }).orElse(null);

    }

    @GET
    @Operation(description = "Hent beregningsgrunnlag for angitt behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "beregningsgrunnlag")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path(PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsgrunnlagDto hentBeregningsgrunnlag(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var opptjening = opptjeningRepository.finnOpptjening(behandling.getId());
        if (opptjening.isEmpty()) {
            return null;
        }

        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = iayTjeneste.finnGrunnlag(behandling.getId());
        return iayGrunnlagOpt.flatMap(iayGrunnlag -> {
            var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandling, iayGrunnlag);
            if (input.isPresent()) {
                return beregningsgrunnlagDtoTjeneste.lagBeregningsgrunnlagDto(input.get());
            } else {
                return Optional.empty();
            }
        }).orElse(null);
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return inputTjenesteProvider.getTjeneste(ytelseType);
    }
}
