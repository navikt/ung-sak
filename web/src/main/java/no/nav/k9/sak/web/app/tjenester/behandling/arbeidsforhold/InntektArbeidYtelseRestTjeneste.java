package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper.SISTE_DAG_I_MARS;

import java.time.LocalDate;
import java.util.Set;

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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.fagsak.MatchFagsak;
import no.nav.k9.sak.kontrakt.frisinn.SøknadsperiodeOgOppgittOpptjeningV2Dto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.web.app.tjenester.fagsak.FagsakApplikasjonTjeneste;
import no.nav.k9.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class InntektArbeidYtelseRestTjeneste {

    public static final String INNTEKT_ARBEID_YTELSE_ARBEIDSFORHOLD_PATH = "/behandling/iay/arbeidsforhold-v2";
    public static final String INNTEKT_ARBEID_YTELSE_INNTEKTSMELDINGER_PATH = "/behandling/iay/im-arbeidsforhold-v2";

    /** TODO: kun FRISINN som bruker denne? */
    public static final String OPPGITT_OPPTJENING_PATH_V2 = "/behandling/oppgitt-opptjening-v2";

    public static final LocalDate FØRSTE_MULIGE_SØKNADPERIODE_START = SISTE_DAG_I_MARS;

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;

    private MapOppgittOpptjening mapOppgittOpptjening;

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
                                           MapOppgittOpptjening mapOppgittOpptjening,
                                           ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste, FagsakTjeneste fagsakTjeneste, FagsakApplikasjonTjeneste fagsakApplikasjonTjeneste, PersoninfoAdapter personinfoAdapter) {
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.mapOppgittOpptjening = mapOppgittOpptjening;
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


    /** TODO: Brukes foreløpig bare av FRISINN. Fjernes, eller generaliseres? */
    @GET
    @Path(OPPGITT_OPPTJENING_PATH_V2)
    @Operation(description = "Hent informasjon om oppgitt opptjening for alle søknadsperioder", summary = ("Returnerer info om oppgitt opptjening og om hvilken ytelser det blir søkt ytelser for."), tags = "oppgitt-opptjening", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer SøknadsperiodeOgOppgittOpptjeningDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SøknadsperiodeOgOppgittOpptjeningV2Dto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SøknadsperiodeOgOppgittOpptjeningV2Dto getOppgittOpptjeningV2(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        if (grunnlag.isEmpty()) {
            // Fins ikke ennå, returnerer tom dto for legacy kompatibilitet med frontend
            return new SøknadsperiodeOgOppgittOpptjeningV2Dto();
        }

        var dto = this.mapOppgittOpptjening.mapOppgittOpptjening(behandling, grunnlag.get());
        return dto;
    }

}
