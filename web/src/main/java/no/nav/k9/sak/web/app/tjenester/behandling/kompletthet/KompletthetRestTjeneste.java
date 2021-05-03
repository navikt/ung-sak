package no.nav.k9.sak.web.app.tjenester.behandling.kompletthet;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Map;
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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdId;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdStatus;
import no.nav.k9.sak.kontrakt.kompletthet.KompletthetsTilstandPåPeriodeDto;
import no.nav.k9.sak.kontrakt.kompletthet.KompletthetsVurderingDto;
import no.nav.k9.sak.kontrakt.kompletthet.Status;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.KompletthetForBeregningTjeneste;

/**
 * Beregningsgrunnlag knyttet til en behandling.
 */
@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class KompletthetRestTjeneste {

    static public final String PATH = "/behandling/psb/kompletthet";
    private BehandlingRepository behandlingRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;

    public KompletthetRestTjeneste() {
        // for resteasy
    }

    @Inject
    public KompletthetRestTjeneste(BehandlingRepository behandlingRepository,
                                   KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
    }

    @GET
    @Operation(description = "Hent tilstand for kompletthet for behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "kompletthet")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public KompletthetsVurderingDto hentBeregningsgrunnlag(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var manglendeVedleggForPeriode = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(BehandlingReferanse.fra(behandling));

        var status = manglendeVedleggForPeriode.entrySet()
            .stream()
            .map(it -> new KompletthetsTilstandPåPeriodeDto(new Periode(it.getKey().getFomDato(), it.getKey().getTomDato()), mapStatusPåInntektsmeldinger(it)))
            .collect(Collectors.toList());

        return new KompletthetsVurderingDto(status);
    }

    private List<ArbeidsgiverArbeidsforholdStatus> mapStatusPåInntektsmeldinger(Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it) {
        return it.getValue()
            .stream()
            .map(at -> new ArbeidsgiverArbeidsforholdStatus(new ArbeidsgiverArbeidsforholdId(at.getArbeidsgiver(), at.getArbeidsforholdId()), Status.MANGLER))
            .collect(Collectors.toList());
    }

}
