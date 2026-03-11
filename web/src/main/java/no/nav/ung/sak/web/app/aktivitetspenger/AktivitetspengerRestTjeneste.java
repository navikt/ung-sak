package no.nav.ung.sak.web.app.aktivitetspenger;

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
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.BeregningsgrunnlagDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.BesteBeregningResultatType;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.PgiÅrsinntektDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerBeregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerBeregningsgrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class AktivitetspengerRestTjeneste {


    public static final String AKTIVITETSPENGER_BASE_PATH = "/aktivitetspenger";
    public static final String BEREGNINGSGRUNNLAG_PATH = AKTIVITETSPENGER_BASE_PATH + "/beregningsgrunnlag";

    private BehandlingRepository behandlingRepository;
    private AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository;

    public AktivitetspengerRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerRestTjeneste(BehandlingRepository behandlingRepository,
                                        AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.aktivitetspengerBeregningsgrunnlagRepository = aktivitetspengerBeregningsgrunnlagRepository;
    }

    @GET
    @Operation(description = "Henter beregningsgrunnlag for en aktivitetspengerbehandling", tags = "avp")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(BEREGNINGSGRUNNLAG_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BeregningsgrunnlagDto> getBeregningsgrunnlag(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return aktivitetspengerBeregningsgrunnlagRepository.hentGrunnlag(behandling.getId())
            .map(AktivitetspengerBeregningsgrunnlag::getBeregningsgrunnlag)
            .map(liste -> liste.stream().map(AktivitetspengerRestTjeneste::mapTilBeregningsgrunnlagDto).toList())
            .orElseThrow(() -> new IllegalStateException("Fant ikke beregningsgrunnlag for behandlingid: "+behandling.getId()));
    }

    private static BeregningsgrunnlagDto mapTilBeregningsgrunnlagDto(Beregningsgrunnlag grunnlag) {
        BeregningInput beregningInput = grunnlag.getBeregningInput().getBeregningInput(grunnlag.getSkjæringstidspunkt());
        PgiKalkulatorInput pgiKalkulatorInput = PgiKalkulator.lagPgiKalkulatorInput(beregningInput);
        Map<Year, BigDecimal> avkortetOgOppjustert = PgiKalkulator.avgrensOgOppjusterÅrsinntekter(pgiKalkulatorInput);

        List<PgiÅrsinntektDto> pgiÅrsinntekter = beregningInput.lagTidslinje().toSegments().stream()
            .map(segment -> {
                Year år = Year.of(segment.getFom().getYear());
                return new PgiÅrsinntektDto(
                    år.getValue(),
                    segment.getValue().getVerdi().setScale(0, RoundingMode.HALF_EVEN),
                    avkortetOgOppjustert.getOrDefault(år, BigDecimal.ZERO).setScale(0, RoundingMode.HALF_EVEN)
                );
            })
            .sorted(Comparator.comparingInt(PgiÅrsinntektDto::årstall))
            .toList();

        return new BeregningsgrunnlagDto(
            grunnlag.getSkjæringstidspunkt(),
            grunnlag.getÅrsinntektAvkortetOppjustertSisteÅr().setScale(0, RoundingMode.HALF_EVEN),
            grunnlag.getÅrsinntektAvkortetOppjustertSisteTreÅr().setScale(0, RoundingMode.HALF_EVEN),
            grunnlag.getBeregnetPrAar().setScale(0, RoundingMode.HALF_EVEN),
            grunnlag.getBeregnetRedusertPrAar().setScale(0, RoundingMode.HALF_EVEN),
            grunnlag.getDagsats().setScale(2, RoundingMode.HALF_EVEN),
            pgiÅrsinntekter,
            mapBesteBeregningResultatType(grunnlag.utledBesteBeregningResultatType())
        );
    }

    private static BesteBeregningResultatType mapBesteBeregningResultatType(no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BesteBeregningResultatType type) {
        return switch (type) {
            case SISTE_ÅR -> BesteBeregningResultatType.SISTE_ÅR;
            case SNITT_SISTE_TRE_ÅR -> BesteBeregningResultatType.SNITT_SISTE_TRE_ÅR;
        };
    }
}
