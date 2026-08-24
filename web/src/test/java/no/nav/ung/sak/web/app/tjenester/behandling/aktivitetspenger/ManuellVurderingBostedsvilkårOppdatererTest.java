package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted.ManuellVurderingBostedsvilkårDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted.VilkårBostedPeriodeVurderingDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.avkort.AvkortTjeneste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ManuellVurderingBostedsvilkårOppdatererTest {

    private static final String SAKSBEHANDLER = "saksbehandlerBosted";

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode PERIODE_2 = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
    private static final Periode PERIODE_3 = new Periode(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private ManuellVurderingBostedsvilkårOppdaterer oppdaterer;
    @Inject
    private AvkortTjeneste avkortTjeneste;

    private Fagsak fagsak;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(SAKSBEHANDLER);
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = repositoryProvider.getFagsakRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);

        oppdaterer = new ManuellVurderingBostedsvilkårOppdaterer(
            behandlingRepository,
            vilkårResultatRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            new HistorikkinnslagRepository(entityManager),
            avkortTjeneste);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1122334455667"), new Saksnummer("BOSTED1"),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        fagsakRepository.opprettNy(fagsak);
    }

    @Test
    void førstegangsbehandling_skal_lagre_bostedvurdering_og_sette_vilkårsresultat_for_vurderte_perioder() {
        var behandling = opprettFørstegangsbehandling(
            ikkeVurdertVilkårsperiode(PERIODE_1),
            ikkeVurdertVilkårsperiode(PERIODE_2));

        var dto = dto(
            oppfylt(PERIODE_1, "bor i Trondheim"),
            ikkeOppfylt(PERIODE_2, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, "flyttet ut av Trondheim"));

        var vilkårResultat = utførOppdatering(behandling, dto);

        var lagredeVurderinger = hentBostedvurderinger(behandling);
        assertThat(lagredeVurderinger).hasSize(2);

        var vurderingPeriode1 = lagredeVurderinger.stream().filter(v -> v.getPeriode().equals(tilDatoIntervallEntitet(PERIODE_1))).findFirst().orElseThrow();
        assertThat(vurderingPeriode1.isGodkjent()).isTrue();
        assertThat(vurderingPeriode1.getIkkeOppfyltÅrsak()).isNull();
        assertThat(vurderingPeriode1.erManuellVurdering()).isTrue();
        assertThat(vurderingPeriode1.getBegrunnelse()).isEqualTo("bor i Trondheim");
        assertThat(vurderingPeriode1.getVurdertAv()).isEqualTo(SAKSBEHANDLER);
        assertThat(vurderingPeriode1.getVurdertTidspunkt()).isNotNull();

        var vurderingPeriode2 = lagredeVurderinger.stream().filter(v -> v.getPeriode().equals(tilDatoIntervallEntitet(PERIODE_2))).findFirst().orElseThrow();
        assertThat(vurderingPeriode2.isGodkjent()).isFalse();
        assertThat(vurderingPeriode2.getIkkeOppfyltÅrsak()).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);
        assertThat(vurderingPeriode2.getFritekstVurderingBrev()).isEqualTo("fritekst " + PERIODE_2.getFom());

        var vilkårPeriode1 = hentVilkårsperiode(vilkårResultat, PERIODE_1);
        assertThat(vilkårPeriode1.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkårPeriode1.getErManueltVurdert()).isTrue();
        assertThat(vilkårPeriode1.getBegrunnelse()).isEqualTo("bor i Trondheim");

        var vilkårPeriode2 = hentVilkårsperiode(vilkårResultat, PERIODE_2);
        assertThat(vilkårPeriode2.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode2.getErManueltVurdert()).isTrue();
        assertThat(vilkårPeriode2.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
    }

    @Test
    void førstegangsbehandling_skal_avvise_vurdering_av_periode_som_ikke_er_til_vurdering() {
        var behandling = opprettFørstegangsbehandling(ikkeVurdertVilkårsperiode(PERIODE_1));

        var dto = dto(oppfylt(PERIODE_2, "periode som ikke er til vurdering"));
        var param = oppdaterParameter(behandling, dto);

        assertThatThrownBy(() -> oppdaterer.oppdater(dto, param))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Forsøker å vurdere perioder som ikke er til vurdering");
    }

    @Test
    void endret_bosted_opphør_skal_skal_tillate_å_lagre_uavhengig_av_vurdering() {
        var originalBehandling = opprettFørstegangsbehandling(
            vilkårsperiode(PERIODE_1, Utfall.OPPFYLT, null, "original periode 1"),
            vilkårsperiode(PERIODE_2, Utfall.IKKE_OPPFYLT, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, "original periode 2"),
            vilkårsperiode(PERIODE_3, Utfall.OPPFYLT, null, "original periode 3"));

        var revurdering = opprettRevurdering(originalBehandling, BehandlingÅrsakType.ENDRET_BOSTED,
            vilkårsperiode(PERIODE_1, Utfall.OPPFYLT, null, "original periode 1 (kopiert grunnlag)"),
            ikkeVurdertVilkårsperiode(PERIODE_2),
            ikkeVurdertVilkårsperiode(PERIODE_3));

        var opphørFraMidtenAvPeriode1 = new Periode(PERIODE_1.getFom().plusDays(15), null);
        var dto = dto(ikkeOppfylt(opphørFraMidtenAvPeriode1, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, "vurdert på nytt i denne behandlingen"));

        var vilkårResultat = utførOppdatering(revurdering, dto);

        var periodeFørOpphør = new Periode(PERIODE_1.getFom(), opphørFraMidtenAvPeriode1.getFom().minusDays(1));
        var vilkårPeriode0 = hentVilkårsperiode(vilkårResultat, periodeFørOpphør);
        assertThat(vilkårPeriode0.getGjeldendeUtfall())
            .as("vurdering gjort i denne behandlingen skal ikke overskrives av forrige behandling")
            .isEqualTo(Utfall.OPPFYLT);
        assertThat(vilkårPeriode0.getBegrunnelse()).isEqualTo("original periode 1 (kopiert grunnlag)");

        // Sjekker at overlapp med periode 1 også får nytt utfall - selv om den ikke var til vurdering
        var periode1_rest = new Periode(opphørFraMidtenAvPeriode1.getFom(), PERIODE_1.getTom());
        var vilkårPeriode1 = hentVilkårsperiode(vilkårResultat, periode1_rest);
        assertThat(vilkårPeriode1.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode1.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
        assertThat(vilkårPeriode1.getBegrunnelse()).isEqualTo("vurdert på nytt i denne behandlingen");

        var vilkårPeriode2 = hentVilkårsperiode(vilkårResultat, PERIODE_2);
        assertThat(vilkårPeriode2.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode2.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
        assertThat(vilkårPeriode2.getBegrunnelse()).isEqualTo("vurdert på nytt i denne behandlingen");

        var vilkårPeriode3 = hentVilkårsperiode(vilkårResultat, PERIODE_3);
        assertThat(vilkårPeriode3.getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        assertThat(vilkårPeriode3.getAvslagsårsak()).isEqualTo(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
        assertThat(vilkårPeriode3.getBegrunnelse()).isEqualTo("vurdert på nytt i denne behandlingen");

        assertThat(vilkårResultat.filterValue(v -> Utfall.IKKE_VURDERT.equals(v.getGjeldendeUtfall())).isEmpty())
            .as("ingen bostedsperioder skal stå igjen som ikke vurdert")
            .isTrue();

        assertThat(hentBostedvurderinger(revurdering))
            .as("kun perioden vurdert i denne behandlingen skal lagres som vurdering på revurderingen")
            .extracting(BostedsvilkårResultatPeriode::getPeriode)
            .containsExactlyInAnyOrder(tilDatoIntervallEntitet(periode1_rest), tilDatoIntervallEntitet(PERIODE_2), tilDatoIntervallEntitet(PERIODE_3));
    }

    private LocalDateTimeline<VilkårPeriode> utførOppdatering(Behandling behandling, ManuellVurderingBostedsvilkårDto dto) {
        var param = oppdaterParameter(behandling, dto);
        oppdaterer.oppdater(dto, param);
        return param.getVilkårResultatBuilder().build().getVilkårTimeline(VilkårType.BOSTEDSVILKÅR);
    }

    private AksjonspunktOppdaterParameter oppdaterParameter(Behandling behandling, ManuellVurderingBostedsvilkårDto dto) {
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        return new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vilkårResultatBuilder, dto);
    }

    private List<BostedsvilkårResultatPeriode> hentBostedvurderinger(Behandling behandling) {
        return inngangsvilkårVurderingRepository.hentGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder)
            .orElseThrow();
    }

    private static VilkårPeriode hentVilkårsperiode(LocalDateTimeline<VilkårPeriode> tidslinje, Periode periode) {
        return tidslinje.segmenter().stream()
            .filter(s -> s.getFom().equals(periode.getFom()) && s.getTom().equals(periode.getTom()))
            .map(s -> s.getValue())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fant ikke vilkårsperiode " + periode + " i " + tidslinje));
    }

    private Behandling opprettFørstegangsbehandling(VilkårsperiodeData... vilkårsperioder) {
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagre(behandling, vilkårsperioder);
        return behandling;
    }

    private Behandling opprettRevurdering(Behandling originalBehandling, BehandlingÅrsakType årsak, VilkårsperiodeData... vilkårsperioder) {
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(årsak))
            .build();
        lagre(revurdering, vilkårsperioder);
        return revurdering;
    }

    private void lagre(Behandling behandling, VilkårsperiodeData... vilkårsperioder) {
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var vilkårResultatBuilder = Vilkårene.builder();
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);
        for (var v : vilkårsperioder) {
            var periodeBuilder = new VilkårPeriodeBuilder()
                .medPeriode(v.periode())
                .medUtfall(v.utfall());
            if (v.avslagsårsak() != null) {
                periodeBuilder.medAvslagsårsak(v.avslagsårsak());
            }
            if (v.begrunnelse() != null) {
                periodeBuilder.medBegrunnelse(v.begrunnelse());
            }
            vilkårBuilder.leggTil(periodeBuilder);
        }
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
    }

    private static ManuellVurderingBostedsvilkårDto dto(VilkårBostedPeriodeVurderingDto... vurderinger) {
        return new ManuellVurderingBostedsvilkårDto(List.of(vurderinger), "begrunnelse for aksjonspunktet");
    }

    private static VilkårBostedPeriodeVurderingDto oppfylt(Periode periode, String begrunnelse) {
        return new VilkårBostedPeriodeVurderingDto(
            new ÅpenPeriode(periode.getFom(), periode.getTom()), true, null, begrunnelse, "fritekst " + periode.getFom());
    }

    private static VilkårBostedPeriodeVurderingDto ikkeOppfylt(Periode periode, BostedsvilkårIkkeOppfyltÅrsak årsak, String begrunnelse) {
        return new VilkårBostedPeriodeVurderingDto(
            new ÅpenPeriode(periode.getFom(), periode.getTom()), false, årsak, begrunnelse, "fritekst " + periode.getFom());
    }

    private static VilkårsperiodeData ikkeVurdertVilkårsperiode(Periode periode) {
        return new VilkårsperiodeData(tilDatoIntervallEntitet(periode), Utfall.IKKE_VURDERT, null, null);
    }

    private static VilkårsperiodeData vilkårsperiode(Periode periode, Utfall utfall, Avslagsårsak avslagsårsak, String begrunnelse) {
        return new VilkårsperiodeData(tilDatoIntervallEntitet(periode), utfall, avslagsårsak, begrunnelse);
    }

    private record VilkårsperiodeData(DatoIntervallEntitet periode, Utfall utfall, Avslagsårsak avslagsårsak, String begrunnelse) {
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
