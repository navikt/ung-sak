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
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatHolder;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandGrunnlagPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandGrunnlagResponseDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bistand.BistandResultatDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

/**
 * REST-tjeneste for å hente bistandsgrunnlag til bruk i VURDER_FAKTA_OM_BISTAND og VURDER_BISTANDSVILKÅR.
 * <p>
 * I motsetning til {@code BostedRestTjeneste} finnes det ingen søknadsfakta-dimensjon for bistand, så
 * responsen består kun av avklaring (foreslått/ferdigstilt), vilkårsresultat og brukerens uttalelse.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BistandRestTjeneste {

    public static final String BISTAND_PATH = "/behandling/bistand";

    private BehandlingRepository behandlingRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private UttalelseRepository uttalelseRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    public BistandRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BistandRestTjeneste(BehandlingRepository behandlingRepository,
                               VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                               UttalelseRepository uttalelseRepository,
                               InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.uttalelseRepository = uttalelseRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
    }

    @GET
    @Path(BISTAND_PATH)
    @Operation(description = "Hent bistandsgrunnlag (avklaringer per periode)", tags = "aktivitetspenger")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BistandGrunnlagResponseDto hentBistandGrunnlag(
        @NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC)
        @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {

        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        long behandlingId = behandling.getId();

        var grunnlagOpt = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR);
        Set<VilkårPeriodeAvklaring> foreslåtte = grunnlagOpt.map(g -> g.getForeslåtteAvklaringer()).orElse(Set.of());
        Set<VilkårPeriodeAvklaring> ferdigstilte = grunnlagOpt.map(g -> g.getFerdigstilteAvklaringer()).orElse(Set.of());

        var avklaringTidslinje = byggAvklaringTidslinje(foreslåtte, ferdigstilte);
        var resultatTidslinje = byggResultatTidslinje(behandlingId);

        Map<UUID, UttalelseV2> uttalelseByReferanse = uttalelseRepository.hentUttalelser(behandlingId, EndringType.AVKLAR_BISTAND).stream()
            .collect(Collectors.toMap(UttalelseV2::getGrunnlagsreferanse, u -> u, (a, _) -> a));

        var kombinert = avklaringTidslinje
            .mapValue(BistandAvklaringOgResultat::new)
            .crossJoin(resultatTidslinje, (interval, avklaring, resultat) -> new LocalDateSegment<>(
                interval,
                (avklaring == null ? new BistandAvklaringOgResultat(null) : avklaring.getValue())
                    .medResultat(resultat == null ? null : resultat.getValue())));

        var perioder = kombinert.stream()
            .filter(segment -> segment.getValue().harAvklaringEllerVilkårsVurdering())
            .map(segment -> {
                var verdi = segment.getValue();
                var avklaring = verdi.getAvklaring();
                var uttalelse = avklaring != null ? uttalelseByReferanse.get(avklaring.getReferanse()) : null;
                boolean harUttalelse = uttalelse != null && uttalelse.harUttalelse();
                String uttalelseTekst = uttalelse != null ? uttalelse.getUttalelseBegrunnelse() : null;

                return new BistandGrunnlagPeriodeDto(
                    segment.getFom(),
                    segment.getTom(),
                    verdi.byggAvklaringDtoHvisFinnes(foreslåtte),
                    verdi.byggResultatDtoHvisFinnes(),
                    harUttalelse,
                    uttalelseTekst
                );
            })
            .collect(Collectors.toList());

        return new BistandGrunnlagResponseDto(perioder);
    }

    private LocalDateTimeline<VilkårPeriodeAvklaring> byggAvklaringTidslinje(Set<VilkårPeriodeAvklaring> foreslåtte, Set<VilkårPeriodeAvklaring> ferdigstilte) {
        var ferdigstiltTidslinje = tilTidslinje(ferdigstilte);
        var foreslåttTidslinje = tilTidslinje(foreslåtte);
        // Foreslåtte avklaringer er nyest og overstyrer ferdigstilte for overlappende perioder
        return ferdigstiltTidslinje.crossJoin(foreslåttTidslinje, (interval, ferdigstilt, foreslått) ->
            new LocalDateSegment<>(interval, foreslått != null ? foreslått.getValue() : ferdigstilt.getValue()));
    }

    private static LocalDateTimeline<VilkårPeriodeAvklaring> tilTidslinje(Set<VilkårPeriodeAvklaring> avklaringer) {
        return new LocalDateTimeline<>(avklaringer.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFomDato(), a.getPeriode().getTomDato(), a))
            .toList(), (interval, lhs, rhs) -> new LocalDateSegment<>(interval, lhs.getValue()));
    }

    private LocalDateTimeline<BistandsvilkårResultatPeriode> byggResultatTidslinje(long behandlingId) {
        List<BistandsvilkårResultatPeriode> vurderinger = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId)
            .flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBistandsvilkårResultatHolder)
            .map(BistandsvilkårResultatHolder::getVurderinger)
            .orElse(List.of());

        return new LocalDateTimeline<>(vurderinger.stream()
            .map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v))
            .toList());
    }

    static class BistandAvklaringOgResultat {

        private final VilkårPeriodeAvklaring avklaring;
        private final BistandsvilkårResultatPeriode resultat;

        BistandAvklaringOgResultat(VilkårPeriodeAvklaring avklaring) {
            this(avklaring, null);
        }

        private BistandAvklaringOgResultat(VilkårPeriodeAvklaring avklaring, BistandsvilkårResultatPeriode resultat) {
            this.avklaring = avklaring;
            this.resultat = resultat;
        }

        BistandAvklaringOgResultat medResultat(BistandsvilkårResultatPeriode resultat) {
            return new BistandAvklaringOgResultat(this.avklaring, resultat);
        }

        boolean harAvklaringEllerVilkårsVurdering() {
            return avklaring != null || resultat != null;
        }

        VilkårPeriodeAvklaring getAvklaring() {
            return avklaring;
        }

        BistandResultatDto byggResultatDtoHvisFinnes() {
            if (resultat == null) {
                return null;
            }
            return new BistandResultatDto(
                resultat.isGodkjent(),
                resultat.getIkkeOppfyltÅrsak(),
                resultat.isManuellVurdering(),
                resultat.getBegrunnelse(),
                resultat.getFritekstVurderingBrev(),
                resultat.getVurdertAv()
            );
        }

        BistandAvklaringDto byggAvklaringDtoHvisFinnes(Set<VilkårPeriodeAvklaring> foreslåtte) {
            if (avklaring == null) {
                return null;
            }
            return new BistandAvklaringDto(
                avklaring.getPeriode().tilPeriode(),
                BistandsvilkårIkkeOppfyltÅrsak.fraKode(avklaring.getIkkeOppfyltÅrsakKode()),
                avklaring.getBegrunnelse(),
                avklaring.skalSendeVarsel(),
                avklaring.getFritekstTilVarsel(),
                avklaring.getBegrunnelseIkkeVarsel(),
                avklaring.getAvklaringtype(),
                foreslåtte.contains(avklaring)
            );
        }
    }
}
