package no.nav.k9.sak.web.app.tjenester.behandling.opptjening;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class MapOpptjeningTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Inject
    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private MapOpptjening mapOpptjening;
    private AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste;
    private AktørId aktørId;
    private Behandling behandling;
    private OpptjeningRepository opptjeningRepository;

    @BeforeEach
    public void setup() {
        behandlingRepository = new BehandlingRepository(entityManager);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        opptjeningRepository = new OpptjeningRepository(entityManager, behandlingRepository, vilkårResultatRepository);
        OpptjeningsperioderTjeneste opptjeningsperioderTjeneste = new OpptjeningsperioderTjeneste(opptjeningRepository, dummyOppgittOpptjeningFilter(), false);
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        mapOpptjening = new MapOpptjening(opptjeningsperioderTjeneste, null, iayTjeneste, vilkårResultatRepository);
        aktørId = AktørId.dummy();
        opprettFagsakOgBehandling();
    }

    @Test
    void skal_mappe_arbeidsforhold_som_starter_på_stp() {
        lagreOpptjeningUtenAktiviteter();
        lagreArbeidsforholdMedPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));

        var opptjeningerDto = mapOpptjening.mapTilOpptjeninger(BehandlingReferanse.fra(behandling));

        assertThat(opptjeningerDto.getOpptjeninger().size()).isEqualTo(1);
        assertThat(opptjeningerDto.getOpptjeninger().get(0).getOpptjeningAktivitetList().size()).isEqualTo(1);
        assertThat(opptjeningerDto.getOpptjeninger().get(0).getOpptjeningAktivitetList().get(0).getOpptjeningFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
    }

    private OppgittOpptjeningFilterProvider dummyOppgittOpptjeningFilter() {
        return new OppgittOpptjeningFilterProvider(new UnitTestLookupInstanceImpl<>(new OppgittOpptjeningFilter() {
        }), behandlingRepository);
    }

    private void opprettFagsakOgBehandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.PSB, aktørId, new Saksnummer("sak"), LocalDate.now(), LocalDate.now());
        var fagsakRepository = new FagsakRepository(entityManager);
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));
    }

    private Opptjening lagreOpptjeningUtenAktiviteter() {
        opptjeningRepository.lagreOpptjeningsperiode(behandling, SKJÆRINGSTIDSPUNKT.minusDays(29), SKJÆRINGSTIDSPUNKT.minusDays(1), true);
        var vilkårBuilder = Vilkårene.builder().hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
        var periodeBuilder = new VilkårPeriodeBuilder();
        vilkårBuilder.leggTil(periodeBuilder.medPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT).medUtfall(Utfall.IKKE_OPPFYLT));
        var vilkårResultatBuilder = Vilkårene.builder();
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
        return opptjeningRepository.lagreOpptjeningResultat(behandling, SKJÆRINGSTIDSPUNKT, Period.of(0, 0, 0), Set.of());
    }

    private void lagreArbeidsforholdMedPeriode(DatoIntervallEntitet periode) {
        var register = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeid = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty()).medAktørId(aktørId);
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty()).medArbeidsgiver(Arbeidsgiver.virksomhet("123456789")).medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
            .medPeriode(periode));
        aktørArbeid.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        register.leggTilAktørArbeid(aktørArbeid);
        iayTjeneste.lagreIayAggregat(behandling.getId(), register);
    }
}
