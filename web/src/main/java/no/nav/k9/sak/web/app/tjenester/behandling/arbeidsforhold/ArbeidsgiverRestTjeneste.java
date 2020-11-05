package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.AktørInntekt;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOpplysningerDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path("")
@Transactional
public class ArbeidsgiverRestTjeneste {

    public static final String ARBEIDSGIVER_PATH = "/behandling/arbeidsgiver";

    private BehandlingRepository behandlingRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public ArbeidsgiverRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public ArbeidsgiverRestTjeneste(BehandlingRepository behandlingRepository,
                                    ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(ARBEIDSGIVER_PATH)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser",
        summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."),
        tags = "inntekt-arbeid-ytelse",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ArbeidsgiverOversiktDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public ArbeidsgiverOversiktDto getArbeidsgiverOpplysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid BehandlingUuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        Set<Arbeidsgiver> arbeidsgivere = new HashSet<>();

        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        grunnlag.ifPresent(iayg -> {
            iayg.getAktørArbeidFraRegister(behandling.getAktørId())
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptyList())
                .stream()
                .map(Yrkesaktivitet::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getBekreftetAnnenOpptjening(behandling.getAktørId())
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .orElse(Collections.emptyList())
                .stream()
                .map(Yrkesaktivitet::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getAktørInntektFraRegister(behandling.getAktørId())
                .map(AktørInntekt::getInntekt)
                .orElse(Collections.emptyList())
                .stream()
                .map(Inntekt::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getAktørYtelseFraRegister(behandling.getAktørId())
                .map(AktørYtelse::getAlleYtelser)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(y -> y.getYtelseGrunnlag().stream())
                .flatMap(g -> g.getYtelseStørrelse().stream())
                .flatMap(s -> s.getVirksomhet().stream().map(Arbeidsgiver::virksomhet))
                .forEach(arbeidsgivere::add);
            iayg.getArbeidsforholdInformasjon()
                .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
                .orElse(Collections.emptyList())
                .stream()
                .map(ArbeidsforholdReferanse::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getArbeidsforholdOverstyringer()
                .stream()
                .map(ArbeidsforholdOverstyring::getArbeidsgiver)
                .filter(Objects::nonNull)
                .forEach(arbeidsgivere::add);
            iayg.getOppgittOpptjening()
                .map(OppgittOpptjening::getEgenNæring)
                .orElse(Collections.emptyList())
                .stream()
                .map(OppgittEgenNæring::getVirksomhetOrgnr)
                .filter(Objects::nonNull)
                .map(Arbeidsgiver::virksomhet)
                .forEach(arbeidsgivere::add);
        });

        var inntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(behandling.getFagsak().getSaksnummer());
        inntektsmeldinger.stream()
            .map(Inntektsmelding::getArbeidsgiver)
            .forEach(arbeidsgivere::add);

        Map<String, ArbeidsgiverOpplysningerDto> oversikt = new HashMap<>();
        arbeidsgivere.stream()
            .map(this::mapFra)
            .collect(Collectors.groupingBy(ArbeidsgiverOpplysningerDto::getReferanse))
            .forEach((key, value) -> oversikt.putIfAbsent(key, value.stream().findFirst().orElseGet(() -> new ArbeidsgiverOpplysningerDto(key, "Ukjent"))));
        return new ArbeidsgiverOversiktDto(oversikt);
    }


    private ArbeidsgiverOpplysningerDto mapFra(Arbeidsgiver arbeidsgiver) {
        try {
            ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            if (arbeidsgiver.getErVirksomhet()) {
                return new ArbeidsgiverOpplysningerDto(arbeidsgiver.getIdentifikator(), opplysninger.getNavn());
            } else {
                return new ArbeidsgiverOpplysningerDto(arbeidsgiver.getIdentifikator(), opplysninger.getIdentifikator(), opplysninger.getNavn(), opplysninger.getFødselsdato());
            }
        } catch (Exception e) {
            return new ArbeidsgiverOpplysningerDto(arbeidsgiver.getIdentifikator(), "Feil ved oppslag");
        }
    }
}
