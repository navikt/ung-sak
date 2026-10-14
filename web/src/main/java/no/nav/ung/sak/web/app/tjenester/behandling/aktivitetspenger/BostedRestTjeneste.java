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
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.bosatt.*;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseV2;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedGrunnlagPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedGrunnlagResponseDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.BostedResultatDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt.BostedsfaktaOgAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt.BostedsfaktaOgAvklaringFletter;

import java.util.List;
import java.util.Set;
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
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private BostedSøknadsfaktaGrunnlagRepository bostedSøknadsfaktaGrunnlagRepository;
    private UttalelseRepository uttalelseRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    public BostedRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BostedRestTjeneste(BehandlingRepository behandlingRepository,
                              VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                              BostedSøknadsfaktaGrunnlagRepository bostedSøknadsfaktaGrunnlagRepository,
                              UttalelseRepository uttalelseRepository, InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.bostedSøknadsfaktaGrunnlagRepository = bostedSøknadsfaktaGrunnlagRepository;
        this.uttalelseRepository = uttalelseRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
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

    @Deprecated // Duplikatimplementasjon av hentBostedGrunnlag
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

        var søknadsfaktaGrunnlag = bostedSøknadsfaktaGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId());
        if (søknadsfaktaGrunnlag.isEmpty()) {
            return new BostedGrunnlagResponseDto(List.of());
        }

        var faktaOgResultat = lagFaktaOgResultatTidslinje(søknadsfaktaGrunnlag.get(), behandling);

        var uttalelser = uttalelseRepository.hentUttalelser(behandling.getId(), EndringType.AVKLAR_BOSTED);
        var uttalelseByReferanse = uttalelser.stream()
            .collect(Collectors.toMap(UttalelseV2::getGrunnlagsreferanse, u -> u, (a, _) -> a));

        var perioder = faktaOgResultat.stream()
            .filter(it -> it.getValue().harAvklaringEllerVilkårsVurdering())
            .map(segment -> {

                var verdi = segment.getValue();
                var faktaOgAvklaring = verdi.getFaktaOgAvklaring();
                var uttalelse = faktaOgAvklaring.getForeslåttAvklaring() != null ? uttalelseByReferanse.get(faktaOgAvklaring.getForeslåttAvklaring().getReferanse()) : null;
                boolean harUttalelse = uttalelse != null && uttalelse.harUttalelse();
                String uttalelseTekst = uttalelse != null ? uttalelse.getUttalelseBegrunnelse() : null;

                var søknadsinformasjon = faktaOgAvklaring.getSøknadsinformasjon();
                var avklaringDto = verdi.byggAvklaringDtoHvisFinnes();
                var resultatDto = verdi.byggResultatDtoHvisFinnes();

                var erBosatt = resultatDto != null ? resultatDto.erBosatt() : null;
                var erIkkeOppfyltÅrsak = resultatDto != null ? resultatDto.ikkeOppfyltÅrsak() : null;

                return new BostedGrunnlagPeriodeDto(
                    segment.getFom(),
                    segment.getTom(),
                    erBosatt,
                    erIkkeOppfyltÅrsak,
                    faktaOgAvklaring.getKilde(),
                    søknadsinformasjon.isErBosattITrondheim(),
                    avklaringDto,
                    resultatDto,
                    harUttalelse,
                    uttalelseTekst
                );
            });

        return new BostedGrunnlagResponseDto(perioder.collect(Collectors.toList()));
    }

    private LocalDateTimeline<BostedFaktaOgResultat> lagFaktaOgResultatTidslinje(BostedSøknadsfaktaGrunnlag søknadsfaktaGrunnlag, Behandling behandling) {
        var avklaringGrunnlag = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BOSTEDSVILKÅR);
        LocalDateTimeline<BostedsfaktaOgAvklaring> faktaOgAvklaringTidslinje = BostedsfaktaOgAvklaringFletter.flettMedAlleAvklaringer(
            søknadsfaktaGrunnlag.hentSøknadsfaktaSomTidslinje(),
            avklaringGrunnlag.map(VilkårsavklaringGrunnlag::getForeslåtteAvklaringer).orElse(Set.of()),
            avklaringGrunnlag.map(VilkårsavklaringGrunnlag::getFerdigstilteAvklaringer).orElse(Set.of()));

        LocalDateTimeline<BostedsvilkårResultatPeriode> vurderingResultatTidslinje = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedTidslinje)
            .orElse(LocalDateTimeline.empty());

        return faktaOgAvklaringTidslinje
            .mapValue(BostedFaktaOgResultat::new)
            .crossJoin(vurderingResultatTidslinje, (interval, fakta, resultat) ->
                new LocalDateSegment<>(interval, fakta.getValue().medResultat(
                    resultat == null ? null : resultat.getValue())));
    }

    static class BostedFaktaOgResultat {

        private final BostedsfaktaOgAvklaring faktaOgAvklaring;
        private final BostedsvilkårResultatPeriode resultat;

        BostedFaktaOgResultat(BostedsfaktaOgAvklaring fakta) {
            this(fakta, null);
        }

        private BostedFaktaOgResultat(BostedsfaktaOgAvklaring faktaOgAvklaring, BostedsvilkårResultatPeriode resultat) {
            this.faktaOgAvklaring = faktaOgAvklaring;
            this.resultat = resultat;
        }

        public BostedFaktaOgResultat medResultat(BostedsvilkårResultatPeriode resultat) {
            return new BostedFaktaOgResultat(this.faktaOgAvklaring, resultat);
        }

        public boolean harAvklaringEllerVilkårsVurdering() {
            return faktaOgAvklaring.getKilde() == Kilde.SAKSBEHANDLER || resultat != null;
        }

        public BostedsfaktaOgAvklaring getFaktaOgAvklaring() {
            return faktaOgAvklaring;
        }

        public BostedsvilkårResultatPeriode getResultat() {
            return resultat;
        }

        public BostedResultatDto byggResultatDtoHvisFinnes() {
            if (resultat == null) {
                return null;
            }
            return new BostedResultatDto(
                resultat.isGodkjent(),
                resultat.getIkkeOppfyltÅrsak(),
                resultat.erManuellVurdering(),
                resultat.getBegrunnelse(),
                resultat.getFritekstVurderingBrev(),
                resultat.getVurdertAv()
            );
        }

        public BostedAvklaringDto byggAvklaringDtoHvisFinnes() {
            if (faktaOgAvklaring == null || !faktaOgAvklaring.harAvklaring()) {
                return null;
            }

            var gjeldendeAvklaring = faktaOgAvklaring.getGjeldendeAvklaring();

            return new BostedAvklaringDto(
                gjeldendeAvklaring.getPeriode().tilPeriode(),
                faktaOgAvklaring.isErBosattITrondheim(),
                BostedsvilkårIkkeOppfyltÅrsak.fraKode(gjeldendeAvklaring.getIkkeOppfyltÅrsakKode()),
                gjeldendeAvklaring.getBegrunnelse(),
                gjeldendeAvklaring.skalSendeVarsel(),
                gjeldendeAvklaring.getFritekstTilVarsel(),
                gjeldendeAvklaring.getBegrunnelseIkkeVarsel(),
                BostedsavklaringKildeType.fraKode(gjeldendeAvklaring.getKildeKode()),
                gjeldendeAvklaring.getKildeFritekst(),
                gjeldendeAvklaring.getAvklaringtype(),
                faktaOgAvklaring.kanRedigeres()
            );
        }
    }

}
