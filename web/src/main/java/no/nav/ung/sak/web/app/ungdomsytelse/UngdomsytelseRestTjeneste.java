package no.nav.ung.sak.web.app.ungdomsytelse;

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
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPerioder;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.beregning.UngdomsytelseSatsPeriodeDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.uttak.UngdomsytelseUttakPeriodeDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@ApplicationScoped
@Transactional
@Path("ungdomsytelse")
@Produces(MediaType.APPLICATION_JSON)
public class UngdomsytelseRestTjeneste {


    private BehandlingRepository behandlingRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    public UngdomsytelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UngdomsytelseRestTjeneste(BehandlingRepository behandlingRepository, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @GET
    @Operation(description = "Henter innvilgede satser for en ungdomsytelsebehandling", tags = "ung")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path("/satser")
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<UngdomsytelseSatsPeriodeDto> getUngdomsytelseInnvilgetSats(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Optional<UngdomsytelseGrunnlag> grunnlag = hentUngdomsytelseGrunnlag(behandlingUuid);
        UngdomsytelseSatsPerioder perioder = grunnlag.map(UngdomsytelseGrunnlag::getSatsPerioder).orElse(null);
        if (perioder == null){
            return Collections.emptyList();
        } else {
            return perioder.getPerioder().stream()
                .map(p->new UngdomsytelseSatsPeriodeDto(
                    p.getPeriode().getFomDato(),
                    p.getPeriode().getTomDato(),
                    p.getDagsats(),
                    p.getGrunnbeløpFaktor(),
                    p.getGrunnbeløp(),
                    p.getSatsType(),
                    p.getAntallBarn(),
                    p.getDagsatsBarnetillegg()))
                .toList();
        }
    }

    @GET
    @Operation(description = "Henter uttaksperioder for en ungdomsytelsebehandling", tags = "ung")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path("/uttak")
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<UngdomsytelseUttakPeriodeDto> getUngdomsytelseUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Optional<UngdomsytelseGrunnlag> grunnlag = hentUngdomsytelseGrunnlag(behandlingUuid);
        UngdomsytelseUttakPerioder uttakPerioder = grunnlag.map(UngdomsytelseGrunnlag::getUttakPerioder).orElse(null);
        if (uttakPerioder == null){
            return Collections.emptyList();
        } else {
            return uttakPerioder.getPerioder().stream()
                .map(p->new UngdomsytelseUttakPeriodeDto(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.getAvslagsårsak()))
                .toList();
        }
    }

    private Optional<UngdomsytelseGrunnlag> hentUngdomsytelseGrunnlag(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.UNGDOMSYTELSE) {
            throw new IllegalArgumentException("Tjenesten virker kun for ungdomsytelse, fikk behandling for annen ytelse");
        }
        Optional<UngdomsytelseGrunnlag> grunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId());
        return grunnlag;
    }


}
