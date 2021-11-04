package no.nav.k9.sak.web.app.tjenester.behandling.kompletthet;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdId;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdIdV2;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdStatus;
import no.nav.k9.sak.kontrakt.kompletthet.ArbeidsgiverArbeidsforholdStatusV2;
import no.nav.k9.sak.kontrakt.kompletthet.KompletthetsTilstandPåPeriodeDto;
import no.nav.k9.sak.kontrakt.kompletthet.KompletthetsTilstandPåPeriodeV2Dto;
import no.nav.k9.sak.kontrakt.kompletthet.KompletthetsVurderingDto;
import no.nav.k9.sak.kontrakt.kompletthet.KompletthetsVurderingV2Dto;
import no.nav.k9.sak.kontrakt.kompletthet.Status;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.KompletthetForBeregningTjeneste;

@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class KompletthetForBeregningRestTjeneste {

    static public final String PATH = "/behandling/kompletthet/beregning";
    static public final String KOMPLETTHET_FOR_BEREGNING_PATH = PATH;
    static public final String KOMPLETTHET_FOR_BEREGNING_PATH_V2 = PATH + "-v2";
    private BehandlingRepository behandlingRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;

    public KompletthetForBeregningRestTjeneste() {
        // for resteasy
    }

    @Inject
    public KompletthetForBeregningRestTjeneste(BehandlingRepository behandlingRepository,
                                               KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
    }

    @GET
    @Operation(description = "Hent tilstand for kompletthet for behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "kompletthet")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(KOMPLETTHET_FOR_BEREGNING_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public KompletthetsVurderingDto utledStatusForKompletthet(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var manglendeVedleggForPeriode = kompletthetForBeregningTjeneste.utledAllePåkrevdeVedleggFraGrunnlag(ref);
        var unikeInntektsmeldingerForFagsak = kompletthetForBeregningTjeneste.hentAlleUnikeInntektsmeldingerForFagsak(behandling.getFagsak().getSaksnummer());

        var status = manglendeVedleggForPeriode.entrySet()
            .stream()
            .map(it -> new KompletthetsTilstandPåPeriodeDto(new Periode(it.getKey().getFomDato(), it.getKey().getTomDato()), mapStatusPåInntektsmeldinger(it, unikeInntektsmeldingerForFagsak, ref)))
            .collect(Collectors.toList());

        return new KompletthetsVurderingDto(status);
    }

    @GET
    @Operation(description = "Hent tilstand for kompletthet for behandling", summary = ("Returnerer beregningsgrunnlag for behandling."), tags = "kompletthet")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(KOMPLETTHET_FOR_BEREGNING_PATH_V2)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public KompletthetsVurderingV2Dto utledStatusForKompletthetV2(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var manglendeVedleggForPeriode = kompletthetForBeregningTjeneste.utledAllePåkrevdeVedleggFraGrunnlag(ref);
        var unikeInntektsmeldingerForFagsak = kompletthetForBeregningTjeneste.hentAlleUnikeInntektsmeldingerForFagsak(behandling.getFagsak().getSaksnummer());

        var status = manglendeVedleggForPeriode.entrySet()
            .stream()
            .map(it -> new KompletthetsTilstandPåPeriodeV2Dto(new Periode(it.getKey().getFomDato(), it.getKey().getTomDato()), mapStatusPåInntektsmeldingerV2(it, unikeInntektsmeldingerForFagsak, ref)))
            .collect(Collectors.toList());

        return new KompletthetsVurderingV2Dto(status);
    }

    private List<ArbeidsgiverArbeidsforholdStatus> mapStatusPåInntektsmeldinger(Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, Set<Inntektsmelding> unikeInntektsmeldingerForFagsak, BehandlingReferanse behandlingReferanse) {
        var resultat = it.getValue()
            .stream()
            .map(at -> new ArbeidsgiverArbeidsforholdStatus(new ArbeidsgiverArbeidsforholdId(at.getArbeidsgiver().getIdentifikator(), at.getArbeidsforholdId()), Status.MANGLER, null))
            .collect(Collectors.toCollection(ArrayList::new));

        resultat.addAll(kompletthetForBeregningTjeneste.utledInntektsmeldingerSomBenytteMotBeregningForPeriode(behandlingReferanse, unikeInntektsmeldingerForFagsak, it.getKey())
            .stream()
            .map(im -> new ArbeidsgiverArbeidsforholdStatus(new ArbeidsgiverArbeidsforholdId(im.getArbeidsgiver().getIdentifikator(),
                im.getEksternArbeidsforholdRef().map(EksternArbeidsforholdRef::getReferanse).orElse(null)), Status.MOTTATT, im.getJournalpostId()))
            .collect(Collectors.toList()));

        return resultat;
    }

    private List<ArbeidsgiverArbeidsforholdStatusV2> mapStatusPåInntektsmeldingerV2(Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, Set<Inntektsmelding> unikeInntektsmeldingerForFagsak, BehandlingReferanse behandlingReferanse) {
        var resultat = it.getValue()
            .stream()
            .map(at -> new ArbeidsgiverArbeidsforholdStatusV2(new ArbeidsgiverArbeidsforholdIdV2(at.getArbeidsgiver(), at.getArbeidsforholdId()), Status.MANGLER, null))
            .collect(Collectors.toCollection(ArrayList::new));

        resultat.addAll(kompletthetForBeregningTjeneste.utledInntektsmeldingerSomBenytteMotBeregningForPeriode(behandlingReferanse, unikeInntektsmeldingerForFagsak, it.getKey())
            .stream()
            .map(im -> new ArbeidsgiverArbeidsforholdStatusV2(new ArbeidsgiverArbeidsforholdIdV2(im.getArbeidsgiver(),
                im.getEksternArbeidsforholdRef().map(EksternArbeidsforholdRef::getReferanse).orElse(null)), Status.MOTTATT, im.getJournalpostId()))
            .collect(Collectors.toList()));

        return resultat;
    }

}
