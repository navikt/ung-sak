package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.YtelseVedtaksbrevRegler;
import no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

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

    private Behandling lagBehandling(AktivitetspengerTestScenario testScenario) {
        var scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(testScenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }
}
