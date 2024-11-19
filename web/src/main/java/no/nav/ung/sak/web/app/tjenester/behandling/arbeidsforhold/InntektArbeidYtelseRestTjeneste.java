package no.nav.ung.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandling.Skjæringstidspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.ung.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.fagsak.MatchFagsak;
import no.nav.ung.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.ung.sak.web.app.tjenester.fagsak.FagsakApplikasjonTjeneste;
import no.nav.ung.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class InntektArbeidYtelseRestTjeneste {

    public static final String INNTEKT_ARBEID_YTELSE_ARBEIDSFORHOLD_PATH = "/behandling/iay/arbeidsforhold-v2";
    public static final String INNTEKT_ARBEID_YTELSE_INNTEKTSMELDINGER_PATH = "/behandling/iay/im-arbeidsforhold-v2";

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;


    private InntektArbeidYtelseTjeneste iayTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private PersoninfoAdapter personinfoAdapter;

    public InntektArbeidYtelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseRestTjeneste(BehandlingRepository behandlingRepository,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste, FagsakTjeneste fagsakTjeneste, FagsakApplikasjonTjeneste fagsakApplikasjonTjeneste, PersoninfoAdapter personinfoAdapter) {
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    @GET
    @Path(INNTEKT_ARBEID_YTELSE_ARBEIDSFORHOLD_PATH)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser", summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."), tags = "inntekt-arbeid-ytelse", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektArbeidYtelseArbeidsforholdV2Dto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Set<InntektArbeidYtelseArbeidsforholdV2Dto> getArbeidsforhold(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        if (grunnlag.isEmpty()) {
            // Fins ikke ennå, returnerer tom dto for legacy kompatibilitet med frontend
            return Set.of();
        }
        InntektArbeidYtelseGrunnlag iayg = grunnlag.get();

        UtledArbeidsforholdParametere param = new UtledArbeidsforholdParametere(
            behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD));
        Skjæringstidspunkt skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        return arbeidsforholdAdministrasjonTjeneste.hentArbeidsforhold(ref, iayg, param);
    }

    @POST
    @Path(INNTEKT_ARBEID_YTELSE_INNTEKTSMELDINGER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent arbeidsforhold fra inntektsmeldinger", summary = ("Returnerer arbeidsforhold oppgitt på inntektsmeldinger"), tags = "inntekt-arbeid-ytelse", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektArbeidYtelseArbeidsforholdV2Dto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Set<InntektArbeidYtelseArbeidsforholdV2Dto> hentArbeidsforholdIdFraInntektsmeldinger(@Parameter(description = "Match-kritierer for å lete opp fagsaken med inntektsmeldinger") @Valid @TilpassetAbacAttributt(supplierClass = FagsakRestTjeneste.MatchFagsakAttributter.class) MatchFagsak dto) {
        var aktørId = personinfoAdapter.hentAktørIdForPersonIdent(dto.getBruker()).orElseThrow(() -> new IllegalArgumentException("Finner ikke aktørId for bruker"));
        var matchendeFagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(dto.getYtelseType(), aktørId, null, null, dto.getPeriode().getFom(), dto.getPeriode().getTom());
        if (matchendeFagsak.isEmpty()) {
            return Set.of();
        }

        return arbeidsforholdAdministrasjonTjeneste.hentArbeidsforholdFraInntektsmeldinger(matchendeFagsak.get().getSaksnummer());
    }

}
