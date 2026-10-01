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
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårsavklaringÅrsaker;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.UttalelseDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsavklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsavklaringVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsavklaringVurderingerDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsavklaringerDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsvurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring.VilkårsvurderingRadDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class VilkårsavklaringRestTjeneste {

    public static final String AVKLARINGER_PATH = "/behandling/vilkar/avklaringer";
    public static final String VURDERINGER_PATH = AVKLARINGER_PATH + "/vurderinger";

    // hentUttalelser filtrerer på typene den får, og returnerer ingenting uten typer
    private static final EndringType[] AVKLARINGSUTTALELSER = {
        EndringType.AVKLAR_BOSTED, EndringType.AVKLAR_BISTAND, EndringType.AVKLAR_ANDRE_LIVSOPPHOLDSYTELSER
    };

    private BehandlingRepository behandlingRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttalelseRepository uttalelseRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;

    public VilkårsavklaringRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VilkårsavklaringRestTjeneste(BehandlingRepository behandlingRepository,
                                        VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                        InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                        VilkårResultatRepository vilkårResultatRepository,
                                        UttalelseRepository uttalelseRepository,
                                        MottatteDokumentRepository mottatteDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.uttalelseRepository = uttalelseRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }

    @GET
    @Path(AVKLARINGER_PATH)
    @Operation(description = "Hent foreslåtte og ferdigstilte vilkårsavklaringer for et vilkår", tags = "aktivitetspenger")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public VilkårsavklaringerDto hentVilkårsavklaringer(
        @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC)
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid,
        @NotNull @QueryParam("vilkarType")
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return hentAvklaringer(behandling, vilkårType);
    }

    @GET
    @Path(VURDERINGER_PATH)
    @Operation(description = "Hent vilkårsvurderinger per foreslått vilkårsavklaring", tags = "aktivitetspenger")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public VilkårsavklaringVurderingerDto hentVilkårsavklaringVurderinger(
        @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC)
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid,
        @NotNull @QueryParam("vilkarType")
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) VilkårType vilkårType) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return hentVurderinger(behandling, vilkårType);
    }

    VilkårsavklaringerDto hentAvklaringer(Behandling behandling, VilkårType vilkårType) {
        validerVilkårType(vilkårType);
        var grunnlag = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), vilkårType);
        if (grunnlag.isEmpty()) {
            return new VilkårsavklaringerDto(vilkårType, List.of());
        }
        var foreslåtte = grunnlag.get().getForeslåtteAvklaringer();
        var foreslåtteReferanser = foreslåtte.stream().map(VilkårPeriodeAvklaring::getReferanse).collect(Collectors.toSet());

        // Etter iverksettelse ligger behandlingens forslag også blant de ferdigstilte, med samme referanse
        var ferdigstilte = grunnlag.get().getFerdigstilteAvklaringer().stream()
            .filter(a -> !foreslåtteReferanser.contains(a.getReferanse()))
            .toList();
        var uttalelser = hentUttalelserPerReferanse(behandling);
        var avklaringer = Stream.concat(
                sortertNyestFørst(foreslåtte).map(a -> tilAvklaringDto(a, true, uttalelser)),
                sortertNyestFørst(ferdigstilte).map(a -> tilAvklaringDto(a, false, uttalelser)))
            .toList();
        return new VilkårsavklaringerDto(vilkårType, avklaringer);
    }

    VilkårsavklaringVurderingerDto hentVurderinger(Behandling behandling, VilkårType vilkårType) {
        validerVilkårType(vilkårType);
        var foreslåtte = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), vilkårType)
            .map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer)
            .orElse(Set.of());
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        LocalDateTimeline<VilkårPeriode> relevanteVilkårsperioder = vilkårene
            .map(v -> VurderingAvVilkårEtterAvklaringTjeneste.relevanteVilkårsperioder(v, vilkårType))
            .orElse(LocalDateTimeline.empty());
        var vurderinger = inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandling.getId(), vilkårType);
        var uttalelser = hentUttalelserPerReferanse(behandling);

        var raderMedAvklaring = foreslåtte.stream()
            .map(avklaring -> {
                var avklaringTidslinje = new LocalDateTimeline<>(avklaring.getPeriode().toLocalDateInterval(), Boolean.TRUE);
                // Hele den avklarte perioden har i praksis samme utfall, så den første vilkårsperioden er representativ
                var utfall = relevanteVilkårsperioder.intersection(avklaringTidslinje).stream()
                    .findFirst()
                    .map(s -> s.getValue().getGjeldendeUtfall())
                    .orElse(Utfall.IKKE_VURDERT);
                var vurdering = vurderinger.intersection(avklaringTidslinje).stream()
                    .map(LocalDateSegment::getValue)
                    .distinct()
                    .max(Comparator.comparing(VilkårsvurderingResultat::vurdertTidspunkt, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .map(VilkårsavklaringRestTjeneste::tilVurderingDto)
                    .orElse(null);
                return new VilkårsvurderingRadDto(
                    avklaring.getPeriode().tilPeriode(),
                    utfall,
                    new VilkårsavklaringVurderingDto(tilAvklaringDto(avklaring, true, uttalelser), vurdering));
            });

        // tilTidslinje tåler overlapp, i tilfelle eldre data har flere foreslåtte avklaringer
        var foreslåtteTidslinje = TidslinjeUtil.tilTidslinje(foreslåtte.stream().map(VilkårPeriodeAvklaring::getPeriode).toList());
        var raderUtenAvklaring = relevanteVilkårsperioder.disjoint(foreslåtteTidslinje).stream()
            .map(s -> new VilkårsvurderingRadDto(new Periode(s.getFom(), s.getTom()), s.getValue().getGjeldendeUtfall(), null));

        var perioder = Stream.concat(raderMedAvklaring, raderUtenAvklaring)
            .sorted(Comparator.comparing((VilkårsvurderingRadDto rad) -> rad.periode().getFom()).reversed())
            .toList();
        return new VilkårsavklaringVurderingerDto(vilkårType, perioder);
    }

    private static void validerVilkårType(VilkårType vilkårType) {
        if (!VilkårsavklaringÅrsaker.alle().containsKey(vilkårType)) {
            throw new IllegalArgumentException("Vilkårtypen har ikke vilkårsavklaring: " + vilkårType);
        }
    }

    private static Stream<VilkårPeriodeAvklaring> sortertNyestFørst(Collection<VilkårPeriodeAvklaring> avklaringer) {
        return avklaringer.stream()
            .sorted(Comparator.comparing((VilkårPeriodeAvklaring a) -> a.getPeriode().getFomDato()).reversed());
    }

    private static VilkårsavklaringDto tilAvklaringDto(VilkårPeriodeAvklaring a, boolean foreslått, Map<UUID, UttalelseDto> uttalelser) {
        return new VilkårsavklaringDto(
            a.getReferanse(),
            a.getPeriode().tilPeriode(),
            a.getAvklaringtype(),
            a.getIkkeOppfyltÅrsakKode(),
            a.getBegrunnelse(),
            a.skalSendeVarsel(),
            a.getFritekstTilVarsel(),
            a.getBegrunnelseIkkeVarsel(),
            a.getKildeKode(),
            a.getKildeFritekst(),
            a.getVurdertAv(),
            a.getVurdertTidspunkt(),
            foreslått,
            uttalelser.get(a.getReferanse()));
    }

    private static VilkårsvurderingDto tilVurderingDto(VilkårsvurderingResultat v) {
        return new VilkårsvurderingDto(
            v.godkjent(),
            v.ikkeOppfyltÅrsak() != null ? v.ikkeOppfyltÅrsak().getKode() : null,
            v.begrunnelse(),
            v.fritekstVurderingBrev(),
            v.erManuellVurdering(),
            v.vurdertAv(),
            v.vurdertTidspunkt());
    }

    private Map<UUID, UttalelseDto> hentUttalelserPerReferanse(Behandling behandling) {
        var uttalelser = uttalelseRepository.hentUttalelser(behandling.getId(), AVKLARINGSUTTALELSER).stream()
            .filter(u -> u.getGrunnlagsreferanse() != null)
            .toList();
        var journalposter = uttalelser.stream()
            .map(UttalelseV2::getSvarJournalpostId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        // HashMap og putIfAbsent fordi Collectors.toMap kaster på null-verdier
        Map<JournalpostId, LocalDateTime> mottattTidspunkt = new HashMap<>();
        mottatteDokumentRepository.hentMottatteDokument(behandling.getFagsakId(), journalposter)
            .forEach(d -> mottattTidspunkt.putIfAbsent(d.getJournalpostId(), d.getMottattTidspunkt()));
        Map<UUID, UttalelseDto> resultat = new HashMap<>();
        uttalelser.forEach(u -> resultat.putIfAbsent(u.getGrunnlagsreferanse(),
            new UttalelseDto(u.harUttalelse(), u.getUttalelseBegrunnelse(), mottattTidspunkt.get(u.getSvarJournalpostId()))));
        return resultat;
    }
}
