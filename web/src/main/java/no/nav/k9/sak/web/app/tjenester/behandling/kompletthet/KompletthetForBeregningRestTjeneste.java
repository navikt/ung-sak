package no.nav.k9.sak.web.app.tjenester.behandling.kompletthet;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
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
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
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
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;
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
    private VilkårResultatRepository vilkårResultatRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    public KompletthetForBeregningRestTjeneste() {
        // for resteasy
    }

    @Inject
    public KompletthetForBeregningRestTjeneste(BehandlingRepository behandlingRepository,
                                               VilkårResultatRepository vilkårResultatRepository,
                                               KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                               @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
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
        var kompletthetPerioder = kompletthetForBeregningTjeneste.hentKompletthetsVurderinger(ref);
        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        var perioderTilVurdering = tjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var innvilgetSøknadsfrist = vilkårResultatRepository.hent(ref.getBehandlingId())
            .getVilkår(VilkårType.SØKNADSFRIST)
            .orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Objects.equals(it.getGjeldendeUtfall(), Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));

        var status = manglendeVedleggForPeriode.entrySet()
            .stream()
            .map(it -> mapV1Periode(ref, unikeInntektsmeldingerForFagsak, kompletthetPerioder, perioderTilVurdering, innvilgetSøknadsfrist, it))
            .collect(Collectors.toList());

        return new KompletthetsVurderingDto(status);
    }

    private KompletthetsTilstandPåPeriodeDto mapV1Periode(BehandlingReferanse ref, Set<Inntektsmelding> unikeInntektsmeldingerForFagsak, List<KompletthetPeriode> kompletthetPerioder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, TreeSet<DatoIntervallEntitet> innvilgetSøknadsfrist, Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it) {
        var kompletthetsvurdering = finnRelevantVurderingForPeriode(it.getKey(), kompletthetPerioder);
        return new KompletthetsTilstandPåPeriodeDto(new Periode(it.getKey().getFomDato(), it.getKey().getTomDato()),
            mapStatusPåInntektsmeldinger(it, unikeInntektsmeldingerForFagsak, ref, kompletthetsvurdering),
            kompletthetsvurdering.map(KompletthetPeriode::getVurdering).orElse(Vurdering.UDEFINERT),
            utledVurdering(it, perioderTilVurdering, innvilgetSøknadsfrist),
            kompletthetsvurdering.map(KompletthetPeriode::getBegrunnelse).orElse(null));
    }

    private Boolean utledVurdering(Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, TreeSet<DatoIntervallEntitet> innvilgetSøknadsfrist) {
        return perioderTilVurdering.stream().anyMatch(at -> it.getKey().equals(at) && !it.getValue().isEmpty()) && innvilgetSøknadsfrist.stream().anyMatch(at -> at.overlapper(it.getKey()));
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
        var kompletthetPerioder = kompletthetForBeregningTjeneste.hentKompletthetsVurderinger(ref);
        var tjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType());
        var perioderTilVurdering = tjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var innvilgetSøknadsfrist = vilkårResultatRepository.hent(ref.getBehandlingId())
            .getVilkår(VilkårType.SØKNADSFRIST)
            .orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())))
            .filter(it -> Objects.equals(it.getGjeldendeUtfall(), Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));

        var status = manglendeVedleggForPeriode.entrySet()
            .stream()
            .map(it -> mapPeriode(ref, unikeInntektsmeldingerForFagsak, kompletthetPerioder, perioderTilVurdering, it, innvilgetSøknadsfrist))
            .collect(Collectors.toList());

        return new KompletthetsVurderingV2Dto(status);
    }

    private KompletthetsTilstandPåPeriodeV2Dto mapPeriode(BehandlingReferanse ref, Set<Inntektsmelding> unikeInntektsmeldingerForFagsak, List<KompletthetPeriode> kompletthetPerioder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, TreeSet<DatoIntervallEntitet> innvilgetSøknadsfrist) {
        var kompletthetsvurdering = finnRelevantVurderingForPeriode(it.getKey(), kompletthetPerioder);
        return new KompletthetsTilstandPåPeriodeV2Dto(new Periode(it.getKey().getFomDato(), it.getKey().getTomDato()),
            mapStatusPåInntektsmeldingerV2(it, unikeInntektsmeldingerForFagsak, ref, kompletthetsvurdering),
            kompletthetsvurdering.map(KompletthetPeriode::getVurdering).orElse(Vurdering.UDEFINERT),
            utledVurdering(it, perioderTilVurdering, innvilgetSøknadsfrist),
            kompletthetsvurdering.map(KompletthetPeriode::getBegrunnelse).orElse(null));
    }

    private List<ArbeidsgiverArbeidsforholdStatus> mapStatusPåInntektsmeldinger(Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, Set<Inntektsmelding> unikeInntektsmeldingerForFagsak, BehandlingReferanse behandlingReferanse, Optional<KompletthetPeriode> kompletthetsvurdering) {
        var resultat = it.getValue()
            .stream()
            .map(at -> new ArbeidsgiverArbeidsforholdStatus(new ArbeidsgiverArbeidsforholdId(at.getArbeidsgiver().getIdentifikator(), at.getArbeidsforholdId()), utledStatus(kompletthetsvurdering), null))
            .collect(Collectors.toCollection(ArrayList::new));

        resultat.addAll(kompletthetForBeregningTjeneste.utledInntektsmeldingerSomBenytteMotBeregningForPeriode(behandlingReferanse, unikeInntektsmeldingerForFagsak, it.getKey())
            .stream()
            .map(im -> new ArbeidsgiverArbeidsforholdStatus(new ArbeidsgiverArbeidsforholdId(im.getArbeidsgiver().getIdentifikator(),
                im.getEksternArbeidsforholdRef().map(EksternArbeidsforholdRef::getReferanse).orElse(null)), Status.MOTTATT, im.getJournalpostId()))
            .toList());

        return resultat;
    }

    private List<ArbeidsgiverArbeidsforholdStatusV2> mapStatusPåInntektsmeldingerV2(Map.Entry<DatoIntervallEntitet, List<ManglendeVedlegg>> it, Set<Inntektsmelding> unikeInntektsmeldingerForFagsak, BehandlingReferanse behandlingReferanse, Optional<KompletthetPeriode> kompletthetsvurdering) {
        var resultat = it.getValue()
            .stream()
            .map(at -> new ArbeidsgiverArbeidsforholdStatusV2(new ArbeidsgiverArbeidsforholdIdV2(at.getArbeidsgiver(), at.getArbeidsforholdId()), utledStatus(kompletthetsvurdering), null))
            .collect(Collectors.toCollection(ArrayList::new));

        resultat.addAll(kompletthetForBeregningTjeneste.utledInntektsmeldingerSomBenytteMotBeregningForPeriode(behandlingReferanse, unikeInntektsmeldingerForFagsak, it.getKey())
            .stream()
            .map(im -> new ArbeidsgiverArbeidsforholdStatusV2(new ArbeidsgiverArbeidsforholdIdV2(im.getArbeidsgiver(),
                im.getEksternArbeidsforholdRef().map(EksternArbeidsforholdRef::getReferanse).orElse(null)), Status.MOTTATT, im.getJournalpostId()))
            .toList());

        return resultat;
    }

    private Status utledStatus(Optional<KompletthetPeriode> kompletthetsvurdering) {
        if (kompletthetsvurdering.isEmpty()) {
            return Status.MANGLER;
        }
        var vurderingPåPeriode = kompletthetsvurdering.get();
        if (Vurdering.KAN_FORTSETTE.equals(vurderingPåPeriode.getVurdering())) {
            return Status.FORTSETT_UTEN;
        }

        return Status.MANGLER;
    }

    private Optional<KompletthetPeriode> finnRelevantVurderingForPeriode(DatoIntervallEntitet key, List<KompletthetPeriode> kompletthetPerioder) {
        var kompletthetsvurderinger = kompletthetPerioder.stream()
            .filter(it -> Objects.equals(key.getFomDato(), it.getSkjæringstidspunkt()))
            .toList();
        if (kompletthetsvurderinger.isEmpty()) {
            return Optional.empty();
        } else if (kompletthetsvurderinger.size() > 1) {
            throw new IllegalStateException("Har flere vurderinger for samme periode " + key + " :: " + kompletthetsvurderinger);
        } else {
            return Optional.of(kompletthetsvurderinger.get(0));
        }
    }

}
