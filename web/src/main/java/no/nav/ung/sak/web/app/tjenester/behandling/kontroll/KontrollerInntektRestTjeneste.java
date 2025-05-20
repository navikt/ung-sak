package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektFilter;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.kontroll.KontrollerInntektDto;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;

import java.util.Collections;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

@Path(KontrollerInntektRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class KontrollerInntektRestTjeneste {


    public static final String BASE_PATH = "/behandling/kontrollerinntekt";
    public static final String KONTROLL_PERIODER_PATH = BASE_PATH;

    private BehandlingRepository behandlingRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private RapportertInntektMapper rapportertInntektMapper;
    private EtterlysningTjeneste etterlysningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    public KontrollerInntektRestTjeneste() {
        // for resteasy
    }

    @Inject
    public KontrollerInntektRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                         TilkjentYtelseRepository tilkjentYtelseRepository,
                                         ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                         RapportertInntektMapper rapportertInntektMapper,
                                         EtterlysningTjeneste etterlysningTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @GET
    @Operation(description = "Henter perioder for kontroll av inntekt", summary = ("Henter perioder for kontroll av inntekt"), tags = "kontroll")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public KontrollerInntektDto hentKontrollerInntekt(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var kontrollertInntektPerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId())
            .stream()
            .flatMap(it -> it.getPerioder().stream())
            .toList();

        final var perioderTilKontroll = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId())
            .filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        final var rapporterteInntekter = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(behandling.getId());
        final var gjeldendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysningTidslinje(behandling.getId(), behandling.getFagsakId(), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        final var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId());

        if (iayGrunnlag.isEmpty()) {
            return new KontrollerInntektDto(Collections.emptyList());
        }

        final var inntekter = iayGrunnlag.get().getRegisterVersjon()
            .map(InntektArbeidYtelseAggregat::getInntekter);
        final var registerinntekter = new InntektFilter(inntekter)
            .getAlleInntekter(InntektsKilde.INNTEKT_UNGDOMSYTELSE);

        return KontrollerInntektMapper.map(kontrollertInntektPerioder,
            rapporterteInntekter,
            registerinntekter,
            gjeldendeEtterlysninger, perioderTilKontroll);
    }


}
