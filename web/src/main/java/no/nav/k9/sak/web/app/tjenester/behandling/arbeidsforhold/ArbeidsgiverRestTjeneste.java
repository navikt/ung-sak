package no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
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
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOpplysningerDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverOversiktDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

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
    @Operation(description = "Henter informasjon om alle arbeidsgivere knyttet til bruker",
        summary = ("Henter informasjon om alle arbeidsgivere (navn, fødselsnr for privat arbeidsgiver osv)"),
        tags = "arbeidsgiver",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer ArbeidsgiverOversiktDto",
                content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ArbeidsgiverOversiktDto.class)))
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public ArbeidsgiverOversiktDto getArbeidsgiverOpplysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) @Valid BehandlingUuidDto uuidDto) {
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
            // MERK: Tar ikke hensyn til vilkårsperioder under vurdering dersom man henter ut aggregat for oppgitt opptjening
            iayg.getOppgittOpptjeningAggregat()
                .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
                .orElse(List.of())
                .stream()
                .map(OppgittOpptjening::getEgenNæring)
                .flatMap(Collection::stream)
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

        var referanser = grunnlag.flatMap(gr -> gr.getArbeidsforholdInformasjon().map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser))
            .orElse(Collections.emptyList());

        arbeidsgivere.stream()
            .map(a -> this.mapFra(a, referanser))
            .collect(Collectors.groupingBy(ArbeidsgiverOpplysningerDto::getIdentifikator))
            .forEach((key, value) -> oversikt.putIfAbsent(key, value.stream().findFirst().orElseGet(() -> new ArbeidsgiverOpplysningerDto(key, "Ukjent", mapReferanserForIdentifikator(referanser, key)))));
        return new ArbeidsgiverOversiktDto(oversikt);
    }


    private ArbeidsgiverOpplysningerDto mapFra(Arbeidsgiver arbeidsgiver, Collection<ArbeidsforholdReferanse> referanser) {
        var identifikator = arbeidsgiver.getIdentifikator();
        try {
            ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            List<ArbeidsforholdIdDto> arbeidsforholdreferanser = mapReferanserForIdentifikator(referanser, identifikator);
            if (arbeidsgiver.getErVirksomhet()) {
                return new ArbeidsgiverOpplysningerDto(identifikator, opplysninger.getNavn(), arbeidsforholdreferanser);
            } else {
                return new ArbeidsgiverOpplysningerDto(identifikator, opplysninger.getAlternativIdentifikator(),
                    opplysninger.getNavn(), opplysninger.getFødselsdato(), arbeidsforholdreferanser);
            }
        } catch (Exception e) {
            return new ArbeidsgiverOpplysningerDto(identifikator, "Feil ved oppslag", mapReferanserForIdentifikator(referanser, identifikator));
        }
    }

    private List<ArbeidsforholdIdDto> mapReferanserForIdentifikator(Collection<ArbeidsforholdReferanse> referanser, String identifikator) {
        return referanser.stream()
            .filter(r -> r.getArbeidsgiver().getIdentifikator().equals(identifikator))
            .map(r -> new ArbeidsforholdIdDto(r.getInternReferanse().getUUIDReferanse(), r.getEksternReferanse().getReferanse()))
            .toList();
    }
}
