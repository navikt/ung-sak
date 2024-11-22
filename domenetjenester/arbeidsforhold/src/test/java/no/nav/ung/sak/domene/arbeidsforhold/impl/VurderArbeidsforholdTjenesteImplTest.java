package no.nav.ung.sak.domene.arbeidsforhold.impl;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.ung.sak.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.ung.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class VurderArbeidsforholdTjenesteImplTest {

    @Inject
    private EntityManager entityManager;

    private IAYRepositoryProvider repositoryProvider;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @BeforeEach
    public void setup() {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt() {
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.UNGDOMSYTELSE);
        var behandling = scenario.lagre(repositoryProvider);
        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        avsluttBehandlingOgFagsak(behandling);
        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_2() {
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.UNGDOMSYTELSE);
        var behandling = scenario.lagre(repositoryProvider);
        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
        var fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private Behandling opprettRevurderingsbehandling(Behandling opprinneligBehandling) {
        var behandlingType = BehandlingType.REVURDERING;
        var revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlingÅrsak(revurderingÅrsak).build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(opprinneligBehandling.getId(), revurdering.getId());
        return revurdering;
    }

}
