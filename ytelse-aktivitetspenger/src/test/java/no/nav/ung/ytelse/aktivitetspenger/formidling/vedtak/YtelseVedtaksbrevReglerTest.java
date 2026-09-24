package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.YtelseVedtaksbrevRegler;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerOpphørScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerRevurderingTestOppsett;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerUendretScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.testdata.BostedsAvklaringTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class YtelseVedtaksbrevReglerTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    @Any
    private YtelseVedtaksbrevRegler vedtaksbrevRegler;

    private AktivitetspengerTestRepositories repositories;

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);

    @BeforeEach
    void setup() {
        repositories = BrevTestUtils.lagAlleTestRepositories(entityManager);
    }


    @Test
    void skal_kun_gi_innvilgelsesbrev_ved_avkortet_bostedsvilkår() {
        var fom = LocalDate.of(2025, 8, 1);
        var tom = LocalDate.of(2025, 11, 30);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetMedAvkortetVilkår(fom, tom, VilkårType.BOSTEDSVILKÅR);

        var behandling = lagBehandling(scenario);

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var vedtaksbrev = totalresultater.vedtaksbrevResultater().getFirst();
        assertThat(vedtaksbrev.dokumentMalType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);
        assertThat(vedtaksbrev.vedtaksbrevBygger()).isInstanceOf(FørstegangsInnvilgelseInnholdBygger.class);
    }

    @Test
    void skal_kun_gi_innvilgelsesbrev_ved_delvis_avslag() {
        var fom = LocalDate.of(2025, 8, 1);
        var tom = LocalDate.of(2025, 11, 30);
        var scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetMedAvslåttVilkår(
            fom, tom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);

        var behandling = lagBehandling(scenario);

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        assertThat(totalresultater.harBrev()).isTrue();
        assertThat(totalresultater.vedtaksbrevResultater()).hasSize(1);

        var vedtaksbrev = totalresultater.vedtaksbrevResultater().getFirst();
        assertThat(vedtaksbrev.dokumentMalType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);
        assertThat(vedtaksbrev.vedtaksbrevBygger()).isInstanceOf(FørstegangsInnvilgelseInnholdBygger.class);
    }

    @Test
    void skal_gi_uendret_brev_når_avklart_vilkår_fortsatt_er_oppfylt_og_vedtaket_er_likt() {
        var scenario = uendretBostedScenario();
        var behandling = AktivitetspengerRevurderingTestOppsett.lagRevurdering(repositories, scenario, scenario);

        var totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        assertThat(malTyper(totalresultater)).containsExactly(DokumentMalType.INGEN_ENDRING);
    }

    @Test
    void skal_ikke_gi_uendret_brev_når_dagsatsen_har_endret_seg() {
        var scenario = uendretBostedScenario();
        var original = AktivitetspengerRevurderingTestOppsett.medEndretDagsats(scenario, BigDecimal.TEN);
        var behandling = AktivitetspengerRevurderingTestOppsett.lagRevurdering(repositories, original, scenario);

        var totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        assertThat(malTyper(totalresultater)).doesNotContain(DokumentMalType.INGEN_ENDRING);
    }

    @Test
    void skal_ikke_gi_uendret_brev_uten_vilkårsavklaring() {
        var scenario = AktivitetspengerRevurderingTestOppsett.utenVilkårsavklaringer(uendretBostedScenario());
        var behandling = AktivitetspengerRevurderingTestOppsett.lagRevurdering(repositories, scenario, scenario);

        var totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        assertThat(malTyper(totalresultater)).doesNotContain(DokumentMalType.INGEN_ENDRING);
    }

    @Test
    void skal_gi_avslagsbrev_og_ikke_uendret_brev_når_avslaget_er_likt_forrige_vedtak() {
        var scenario = AktivitetspengerOpphørScenarioer.opphørPgaBosted(FOM);
        var behandling = AktivitetspengerRevurderingTestOppsett.lagRevurdering(repositories, scenario, scenario);

        var totalresultater = vedtaksbrevRegler.kjør(behandling.getId());

        assertThat(malTyper(totalresultater)).containsExactly(DokumentMalType.OPPHØR_DOK);
    }

    private static AktivitetspengerTestScenario uendretBostedScenario() {
        var vurdertPeriode = new Periode(FOM.plusMonths(3), FOM.plusWeeks(52).minusDays(1));
        return AktivitetspengerUendretScenarioer.uendretScenario(FOM,
            BostedsAvklaringTestData.opphør(vurdertPeriode, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            null);
    }

    private static List<DokumentMalType> malTyper(BehandlingVedtaksbrevResultat totalresultater) {
        return totalresultater.vedtaksbrevResultater().stream()
            .map(Vedtaksbrev::dokumentMalType)
            .toList();
    }

    private Behandling lagBehandling(AktivitetspengerTestScenario testScenario) {
        var scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(testScenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }
}
