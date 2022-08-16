package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.søknadsfrist.SøknadsfristTilstandDto;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class SøknadsfristRestTjeneste {

    private static final String PATH = "/behandling/søknadsfrist/status";
    public static final String SØKNADSFRIST_STATUS_PATH = PATH;

    private BehandlingRepository behandlingRepository;
    private SøknadsfristTjenesteProvider søknadsfristTjenesteProvider;
    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;
    private boolean nymapping = false;

    public SøknadsfristRestTjeneste() {
    }

    @Inject
    public SøknadsfristRestTjeneste(BehandlingRepository behandlingRepository,
                                    SøknadsfristTjenesteProvider søknadsfristTjenesteProvider,
                                    AvklartSøknadsfristRepository avklartSøknadsfristRepository,
                                    @KonfigVerdi(value = "soknadsfrist.benytt.eldstekrav.per.periode", defaultVerdi = "false", required = false) boolean nymapping) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenesteProvider = søknadsfristTjenesteProvider;
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
        this.nymapping = nymapping;
    }

    @GET
    @Operation(description = "Hent status på søknadsfrist", summary = ("Returnerer status for søknadsfrist."), tags = "søknadsfrist")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(SØKNADSFRIST_STATUS_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SøknadsfristTilstandDto utledStatus(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var referanse = BehandlingReferanse.fra(behandling);
        var vurderSøknadsfristTjeneste = søknadsfristTjenesteProvider.finnVurderSøknadsfristTjeneste(referanse);
        var avklartSøknadsfristResultat = avklartSøknadsfristRepository.hentHvisEksisterer(referanse.getBehandlingId());

        Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> relevanteVurderteKravdokumentMedPeriodeForBehandling = vurderSøknadsfristTjeneste.relevanteVurderteKravdokumentMedPeriodeForBehandling(referanse);

        // Tar ut OMP fra denne inntil løsning for å støtte det bedre er på plass
        if (nymapping && !Objects.equals(FagsakYtelseType.OMSORGSPENGER, behandling.getFagsakYtelseType())) {
            var kravrekkefølge = vurderSøknadsfristTjeneste.utledKravrekkefølge(referanse);
            var behandlingsLinje = new LocalDateTimeline<>(relevanteVurderteKravdokumentMedPeriodeForBehandling.values()
                .stream()
                .flatMap(Collection::stream)
                .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
                .toList(), StandardCombinators::alwaysTrueForMatch);

            // Tar her første dokumentet som kom inn for en gitt periode, dette fungerer ikke for refusjonskrav fra AG for OMP
            var kravrekkefølgeForPerioderIBehandlingen = kravrekkefølge.intersection(behandlingsLinje, ((localDateInterval, leftSegment, rightSegment) -> tilKravdokument(leftSegment)));

            var kravDokumentListMap = vurderSøknadsfristTjeneste.vurderSøknadsfrist(referanse)
                .entrySet()
                .stream()
                .filter(it -> kravrekkefølgeForPerioderIBehandlingen.stream()
                    .anyMatch(at -> Objects.equals(at.getValue(), it.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            return new MapTilSøknadsfristDto().mapTilV2(kravDokumentListMap, avklartSøknadsfristResultat, kravrekkefølgeForPerioderIBehandlingen);
        } else {

            return new MapTilSøknadsfristDto().mapTil(relevanteVurderteKravdokumentMedPeriodeForBehandling, avklartSøknadsfristResultat);
        }
    }

    private LocalDateSegment<KravDokument> tilKravdokument(LocalDateSegment<List<KravDokument>> leftSegment) {
        if (leftSegment == null) {
            return null;
        }
        var førsteDokument = leftSegment.getValue().stream().min(Comparator.naturalOrder()).orElseThrow();
        return new LocalDateSegment<>(leftSegment.getLocalDateInterval(), førsteDokument);
    }

}
