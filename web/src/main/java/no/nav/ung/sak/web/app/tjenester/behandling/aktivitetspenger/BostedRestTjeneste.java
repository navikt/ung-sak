package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BosattSøknadGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedGrunnlagPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedGrunnlagResponseDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

/**
 * REST-tjeneste for å hente bostedsgrunnlag til bruk i VURDER_BOSTED og MANUELL_VURDERING_BOSTEDSVILKÅR.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BostedRestTjeneste {

    public static final String BOSATT_PATH = "/behandling/bosatt";
    public static final String BOSATT_FAKTA_PATH = "/behandling/bosatt-fakta";

    private BehandlingRepository behandlingRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private BosattSøknadGrunnlagRepository bosattSøknadGrunnlagRepository;
    private UttalelseRepository uttalelseRepository;

    public BostedRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BostedRestTjeneste(BehandlingRepository behandlingRepository,
                               BostedsGrunnlagRepository bostedsGrunnlagRepository,
                               BosattSøknadGrunnlagRepository bosattSøknadGrunnlagRepository,
                               UttalelseRepository uttalelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.bosattSøknadGrunnlagRepository = bosattSøknadGrunnlagRepository;
        this.uttalelseRepository = uttalelseRepository;
    }

    @GET
    @Path(BOSATT_PATH)
    @Operation(description = "Hent bostedsgrunnlag (avklaringer per periode)", tags = "aktivitetspenger")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BostedGrunnlagResponseDto hentBostedGrunnlag(
        @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC)
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return hentBostedGrunnlagInternal(behandlingUuid);
    }

    @GET
    @Path(BOSATT_FAKTA_PATH)
    @Operation(description = "Hent bostedsgrunnlag (avklaringer per periode)", tags = "aktivitetspenger")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BostedGrunnlagResponseDto hentBosattFakta(
        @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC)
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return hentBostedGrunnlagInternal(behandlingUuid);
    }

    private BostedGrunnlagResponseDto hentBostedGrunnlagInternal(BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var grunnlagOpt = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId());
        if (grunnlagOpt.isEmpty()) {
            return new BostedGrunnlagResponseDto(List.of());
        }

        var grunnlag = grunnlagOpt.get();

        // Hent søknadsdata fra separat aggregat
        Map<LocalDate, Boolean> søknadErBosattPerFom = bosattSøknadGrunnlagRepository.hentSøknadBostedPerFom(behandling.getId());

        // Hent bosteduttalelser og indekser dem på periode fom-dato
        var periodeReferanser = grunnlag.getHolder().getPeriodeAvklaringer().stream()
            .map(BostedsPeriodeAvklaring::getReferanse)
            .collect(Collectors.toSet());
        var uttalelser = uttalelseRepository.hentUttalelser(behandling.getId(), EndringType.AVKLAR_BOSTED);
        var uttalelseByFom = uttalelser.stream()
            .filter(u -> periodeReferanser.contains(u.getGrunnlagsreferanse()))
            .collect(Collectors.toMap(u -> u.getPeriode().getFomDato(), u -> u, (a, b) -> a));

        // Bygg liste med én DTO per vilkårsperiode (skjæringstidspunkt)
        var perioder = new ArrayList<BostedGrunnlagPeriodeDto>();
        for (BostedsPeriodeAvklaring periodeAvklaring : grunnlag.getHolder().getPeriodeAvklaringer()) {
            LocalDate fom = periodeAvklaring.getSkjæringstidspunkt();

            Boolean søknadOppgitt = søknadErBosattPerFom.get(fom);

            var uttalelse = uttalelseByFom.get(fom);
            boolean harUttalelse = uttalelse != null && uttalelse.harUttalelse();
            String uttalelseTekst = uttalelse != null ? uttalelse.getUttalelseBegrunnelse() : null;

            perioder.add(new BostedGrunnlagPeriodeDto(
                fom,
                periodeAvklaring.isErBosattITrondheim(),
                periodeAvklaring.getFraflyttingsDato(),
                periodeAvklaring.getFraflyttingsÅrsak(),
                periodeAvklaring.getKilde(),
                søknadOppgitt,
                harUttalelse,
                uttalelseTekst
            ));
        }

        return new BostedGrunnlagResponseDto(perioder);
    }
}
