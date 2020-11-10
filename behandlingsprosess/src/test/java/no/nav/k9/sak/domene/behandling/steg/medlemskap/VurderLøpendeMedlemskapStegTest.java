package no.nav.k9.sak.domene.behandling.steg.medlemskap;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapBuilder;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.medlemskap.VurderLøpendeMedlemskap;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class VurderLøpendeMedlemskapStegTest {

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = provider.getBehandlingRepository();
    private MedlemskapRepository medlemskapRepository = provider.getMedlemskapRepository();
    private PersonopplysningRepository personopplysningRepository = provider.getPersonopplysningRepository();
    private FagsakRepository fagsakRepository = provider.getFagsakRepository();

    private VurderMedlemskapSteg steg;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private VurderLøpendeMedlemskap vurdertLøpendeMedlemskapTjeneste;

    @BeforeEach
    public void setUp() {
        steg = new VurderMedlemskapSteg(vurdertLøpendeMedlemskapTjeneste, provider);
    }

    @Test
    public void skal_gi_avslag() {
        // Arrange
        LocalDate datoMedEndring = LocalDate.now().plusDays(10);
        LocalDate ettÅrSiden = LocalDate.now().minusYears(1);
        LocalDate iDag = LocalDate.now();

        var scenario = TestScenarioBuilder.builderMedSøknad();
        MedlemskapPerioderEntitet periode = opprettPeriode(ettÅrSiden, iDag, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);

        Behandling behandling = scenario.lagre(provider);
        PersonInformasjonBuilder personInformasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        PersonInformasjonBuilder.PersonstatusBuilder personstatusBuilder = personInformasjonBuilder.getPersonstatusBuilder(scenario.getDefaultBrukerAktørId(),
            DatoIntervallEntitet.fraOgMed(ettÅrSiden));
        personstatusBuilder.medPersonstatus(PersonstatusType.BOSA);
        personInformasjonBuilder.leggTil(personstatusBuilder);

        personopplysningRepository.lagre(behandling.getId(), personInformasjonBuilder);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);
        VilkårResultatBuilder inngangsvilkårBuilder = Vilkårene.builder();
        var vilkårBuilder = inngangsvilkårBuilder.hentBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(ettÅrSiden, datoMedEndring).medUtfall(Utfall.OPPFYLT));
        inngangsvilkårBuilder.leggTil(vilkårBuilder);
        Vilkårene vilkårene = inngangsvilkårBuilder.build();

        provider.getVilkårResultatRepository().lagre(revudering.getId(), vilkårene);
        oppdaterMedlem(datoMedEndring, periode, revudering.getId());

        VurdertMedlemskapPeriodeEntitet.Builder builder = new VurdertMedlemskapPeriodeEntitet.Builder();

        VurdertLøpendeMedlemskapBuilder builderIkkeOk = builder.getBuilderFor(datoMedEndring);
        builderIkkeOk.medBosattVurdering(false);
        builderIkkeOk.medOppholdsrettVurdering(false);
        builderIkkeOk.medLovligOppholdVurdering(false);

        builder.leggTil(builderIkkeOk);

        VurdertMedlemskapPeriodeEntitet hvaSkalLagres = builder.build();
        medlemskapRepository.lagreLøpendeMedlemskapVurdering(revudering.getId(), hvaSkalLagres);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revudering);
        Fagsak fagsak = revudering.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        steg.utførSteg(kontekst);

        final var nyeVilkårene = provider.getVilkårResultatRepository().hentHvisEksisterer(revudering.getId());
        assertThat(nyeVilkårene).isPresent();
        Vilkårene grunnlag = nyeVilkårene.get();
        List<VilkårPeriode> ikkeOppfylt = grunnlag.getVilkårene().stream().filter(it -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(it.getVilkårType()))
            .map(Vilkår::getPerioder).flatMap(Collection::stream).filter(p -> p.getGjeldendeUtfall().equals(Utfall.IKKE_OPPFYLT))
            .collect(Collectors.toList());
        assertThat(ikkeOppfylt).hasSize(1);
    }

    private Behandling opprettRevudering(Behandling behandling) {
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA);

        Behandling revudering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(revurderingÅrsak).build();
        Long behandlingId = behandling.getId();

        Long revurderingId = behandlingRepository.lagre(revudering, behandlingRepository.taSkriveLås((Long) null));

        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(behandlingId, revurderingId);

        return revudering;
    }

    private MedlemskapPerioderEntitet opprettPeriode(LocalDate fom, LocalDate tom, MedlemskapDekningType dekningType) {
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
            .medDekningType(dekningType)
            .medMedlemskapType(MedlemskapType.FORELOPIG)
            .medKildeType(MedlemskapKildeType.MEDL)
            .medPeriode(fom, tom)
            .medMedlId(1L)
            .build();
        return periode;
    }

    private void avslutterBehandlingOgFagsak(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.AVSLUTTET);
    }

    private void oppdaterMedlem(LocalDate datoMedEndring, MedlemskapPerioderEntitet periode, Long behandlingId) {
        MedlemskapPerioderEntitet nyPeriode = new MedlemskapPerioderBuilder()
            .medPeriode(datoMedEndring, null)
            .medDekningType(MedlemskapDekningType.FULL)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medKildeType(MedlemskapKildeType.MEDL)
            .medMedlId(2L)
            .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, asList(periode, nyPeriode));
    }
}
