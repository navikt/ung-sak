package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevEditorResponse;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjon;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjonType;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tester at ulike flyter fra klient funker.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevTjenesteEditorTest {

    @Inject
    private VedtaksbrevTjeneste vedtaksbrevTjeneste;
    @Inject
    private EntityManager entityManager;

    private UngTestRepositories ungTestRepositories;

    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }

    @Test
    void skal_lage_editor_response() {
        UngTestScenario ungTestscenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestscenario, ungTestRepositories);

        //Initielle valg - kun automatisk brev
        VedtaksbrevEditorResponse response = vedtaksbrevTjeneste.editor(behandling.getId(), DokumentMalType.ENDRING_INNTEKT, false);
        List<VedtaksbrevSeksjon> seksjoner = response.original();
        assertThat(seksjoner).hasSize(4);
        var stiler = seksjoner.stream().filter(s -> s.type() == VedtaksbrevSeksjonType.STYLE)
            .findFirst().orElseThrow();

        assertThat(stiler.innhold()).contains("<style>");


    }
}
