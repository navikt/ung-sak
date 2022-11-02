package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.PSBEndringPåForlengelseInput;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class HarEndretKompletthetVurdererTest {

    @Inject
    private EntityManager entityManager;

    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private HarEndretKompletthetVurderer harEndretKompletthetVurderer;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private Behandling behandling;

    HarEndretKompletthetVurdererTest() {
    }


    @BeforeEach
    void setUp() {
        beregningPerioderGrunnlagRepository = new BeregningPerioderGrunnlagRepository(entityManager, new VilkårResultatRepository(entityManager));
        harEndretKompletthetVurderer = new HarEndretKompletthetVurderer(beregningPerioderGrunnlagRepository);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandling = opprettBehandling(LocalDate.now());
    }

    @Test
    void skal_gi_ingen_endring_ved_kun_initiell_versjon() {
        var STP = LocalDate.now();
        List<KompletthetPeriode> kompletthetPerioder = List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, STP, "begrunnelse"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder);

        var input = new PSBEndringPåForlengelseInput(BehandlingReferanse.fra(behandling));
        var harEndretVurdering = harEndretKompletthetVurderer.harKompletthetMedEndretVurdering(
            input,
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(3)));

        assertThat(harEndretVurdering).isFalse();
    }

    @Test
    void skal_gi_ingen_endring_ved_initiell_versjon_lik_aktivt() {
        var STP = LocalDate.now();
        List<KompletthetPeriode> kompletthetPerioder = List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, STP, "begrunnelse"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder);


        List<KompletthetPeriode> kompletthetPerioder2 = List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, STP, "begrunnelse2"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder2);


        var input = new PSBEndringPåForlengelseInput(BehandlingReferanse.fra(behandling));
        var harEndretVurdering = harEndretKompletthetVurderer.harKompletthetMedEndretVurdering(
            input,
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(3)));

        assertThat(harEndretVurdering).isFalse();
    }

    @Test
    void skal_gi_endring_ved_initiell_versjon_ulik_aktivt() {
        var STP = LocalDate.now();
        List<KompletthetPeriode> kompletthetPerioder = List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, STP, "begrunnelse"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder);


        List<KompletthetPeriode> kompletthetPerioder2 = List.of(new KompletthetPeriode(Vurdering.MANGLENDE_GRUNNLAG, STP, "begrunnelse2"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder2);


        var input = new PSBEndringPåForlengelseInput(BehandlingReferanse.fra(behandling));
        var harEndretVurdering = harEndretKompletthetVurderer.harKompletthetMedEndretVurdering(
            input,
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(3)));

        assertThat(harEndretVurdering).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_ved_initiell_versjon_lik_aktivt_endret_fram_og_tilbake() {
        var STP = LocalDate.now();
        List<KompletthetPeriode> kompletthetPerioder = List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, STP, "begrunnelse"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder);
        
        List<KompletthetPeriode> kompletthetPerioder2 = List.of(new KompletthetPeriode(Vurdering.MANGLENDE_GRUNNLAG, STP, "begrunnelse2"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder2);

        List<KompletthetPeriode> kompletthetPerioder3 = List.of(new KompletthetPeriode(Vurdering.KAN_FORTSETTE, STP, "begrunnelse3"));
        beregningPerioderGrunnlagRepository.lagre(behandling.getId(), kompletthetPerioder3);


        var input = new PSBEndringPåForlengelseInput(BehandlingReferanse.fra(behandling));
        var harEndretVurdering = harEndretKompletthetVurderer.harKompletthetMedEndretVurdering(
            input,
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(3)));

        assertThat(harEndretVurdering).isFalse();
    }


    private Behandling opprettBehandling(@SuppressWarnings("unused") LocalDate skjæringstidspunkt) {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            AktørId.dummy(), new Saksnummer("SAK"), skjæringstidspunkt, skjæringstidspunkt.plusDays(3));
        @SuppressWarnings("unused")
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
