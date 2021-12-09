package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class PSBPreconditionBeregningAksjonspunktUtlederTest {

    public static final LocalDate STP = LocalDate.now();
    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
    private Behandling behandling;
    private Fagsak fagsak;

    private PSBPreconditionBeregningAksjonspunktUtleder utleder;


    @BeforeEach
    public void setup() {

        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"), STP, STP.plusDays(10));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        when(perioderTilVurderingTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .thenReturn(new TreeSet<>((Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(STP, STP.plusDays(10))))));
        utleder = new PSBPreconditionBeregningAksjonspunktUtleder(iayTjeneste, perioderTilVurderingTjeneste, fagsakRepository, true);
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_med_toggle_av() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP));

        utleder = new PSBPreconditionBeregningAksjonspunktUtleder(iayTjeneste, perioderTilVurderingTjeneste, fagsakRepository, false);
        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isZero();
    }

    @Test
    void skal_returnere_aksjonspunkt_ved_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_returnere_aksjonspunkt_ved_overlapp_der_sakinfotrygdmigrering_finnes_fra_før() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_returnere_aksjonspunkt_ved_overlapp_og_oppdatere_eksisterende_sakinfotrygdmigrering() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.plusDays(2)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.plusDays(2)));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_med_overlapp_for_periode_som_ikke_vurderes() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)));
        fagsakRepository.lagreOgFlush(new SakInfotrygdMigrering(fagsak.getId(), STP.minusDays(10)));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP.minusDays(10));
    }

    @Test
    void skal_ikkje_returnere_aksjonspunkt_uten_overlapp() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(10)));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(0);
        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isZero();
    }

    @Test
    void skal_returnere_aksjonspunkt_for_kant_i_kant() {
        lagInfotrygdPsbYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(STP.minusMonths(1), STP.minusDays(1)));

        var aksjonspunkter = utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, STP)));

        assertThat(aksjonspunkter.size()).isEqualTo(1);
        assertThat(aksjonspunkter.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.OVERSTYR_BEREGNING_INPUT);
        var sakInfotrygdMigrering = fagsakRepository.hentSakInfotrygdMigreringer(fagsak.getId());
        assertThat(sakInfotrygdMigrering.size()).isEqualTo(1);
        assertThat(sakInfotrygdMigrering.get(0).getSkjæringstidspunkt()).isEqualTo(STP);
    }

    private void lagInfotrygdPsbYtelse(DatoIntervallEntitet periode) {
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder.oppdatere(Optional.empty());
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty());

        var anvistBuilder = ytelseBuilder.getAnvistBuilder();
        var ytelseAnvist = anvistBuilder.medAnvistPeriode(periode)
            .medBeløp(BigDecimal.TEN)
            .medDagsats(BigDecimal.TEN)
            .medUtbetalingsgradProsent(BigDecimal.valueOf(100))
            .build();
        ytelseBuilder.medYtelseAnvist(ytelseAnvist)
            .medPeriode(periode)
            .medKilde(Fagsystem.INFOTRYGD)
            .medYtelseType(FagsakYtelseType.PSB);

        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder)
            .medAktørId(fagsak.getAktørId());
        iayBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), iayBuilder);
    }
}
