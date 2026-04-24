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
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsAvklaringHolder;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlag;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
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
 * REST-tjeneste for å hente bostedsgrunnlag til bruk i VURDER_BOSTED og FASTSETT_BOSTED.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BostedRestTjeneste {

    public static final String BOSATT_PATH = "/behandling/bosatt";

    private BehandlingRepository behandlingRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private UttalelseRepository uttalelseRepository;

    public BostedRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BostedRestTjeneste(BehandlingRepository behandlingRepository,
                               BostedsGrunnlagRepository bostedsGrunnlagRepository,
                               UttalelseRepository uttalelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.bostedsGrunnlagRepository = bostedsGrunnlagRepository;
        this.uttalelseRepository = uttalelseRepository;
    }

    @GET
    @Path(BOSATT_PATH)
    @Operation(description = "Hent bostedsgrunnlag (foreslåtte og fastsatte avklaringer per periode)", tags = "aktivitetspenger")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BostedGrunnlagResponseDto hentBostedGrunnlag(
        @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC)
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {

        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var grunnlagOpt = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId());
        if (grunnlagOpt.isEmpty()) {
            return new BostedGrunnlagResponseDto(List.of());
        }

        var grunnlag = grunnlagOpt.get();
        var fastsatte = grunnlag.getFastsattHolder() != null
            ? mapAvklaringer(grunnlag.getFastsattHolder())
            : Map.<LocalDate, Boolean>of();
        var søknadOppgitte = grunnlag.getSøknadHolder() != null
            ? mapAvklaringer(grunnlag.getSøknadHolder())
            : Map.<LocalDate, Boolean>of();

        // Hent bosteduttalelser og indekser dem på periode fom-dato
        var uttalelser = uttalelseRepository.hentUttalelser(behandling.getId(), EndringType.AVKLAR_BOSTED);
        var uttalelseByFom = uttalelser.stream()
            .filter(u -> grunnlag.getGrunnlagsreferanse().equals(u.getGrunnlagsreferanse()))
            .collect(Collectors.toMap(u -> u.getPeriode().getFomDato(), u -> u, (a, b) -> a));

        // Bygg liste med én DTO per avklaringsperiode
        var perioder = new ArrayList<BostedGrunnlagPeriodeDto>();
        for (BostedsAvklaring avklaring : grunnlag.getForeslåttHolder().getAvklaringer()) {
            var fom = avklaring.getFomDato();
            var fastsatt = fastsatte.get(fom);
            var søknadOppgitt = søknadOppgitte.get(fom);
            var dto = new BostedGrunnlagPeriodeDto(fom, avklaring.erBosattITrondheim(), fastsatt, søknadOppgitt);

            // Legg ved uttalelse fra bruker dersom den finnes for perioden
            var uttalelse = finnUttalelseForDato(uttalelseByFom, fom);
            if (uttalelse != null) {
                dto.medUttalelse(uttalelse.harUttalelse(), uttalelse.getUttalelseBegrunnelse());
            }

            perioder.add(dto);
        }

        return new BostedGrunnlagResponseDto(perioder);
    }

    /**
     * Finner uttalelse der gitt dato er innenfor uttalelsens periode.
     * Brukes for å håndtere at en vilkårperiode kan ha split avklaringer med ulike fom-datoer.
     */
    private static UttalelseV2 finnUttalelseForDato(Map<LocalDate, UttalelseV2> uttalelseByFom, LocalDate dato) {
        // Sjekk eksakt treff først
        if (uttalelseByFom.containsKey(dato)) {
            return uttalelseByFom.get(dato);
        }
        // Finn uttalelse der dato faller innenfor perioden (for splittet avklaring)
        return uttalelseByFom.values().stream()
            .filter(u -> !dato.isBefore(u.getPeriode().getFomDato()) && !dato.isAfter(u.getPeriode().getTomDato()))
            .findFirst()
            .orElse(null);
    }

    private static Map<LocalDate, Boolean> mapAvklaringer(BostedsAvklaringHolder holder) {
        return holder.getAvklaringer().stream()
            .collect(Collectors.toMap(BostedsAvklaring::getFomDato, BostedsAvklaring::erBosattITrondheim));
    }
}
