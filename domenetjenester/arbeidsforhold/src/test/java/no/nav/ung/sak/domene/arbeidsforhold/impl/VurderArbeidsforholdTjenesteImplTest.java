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

    private static final LocalDate IDAG = LocalDate.now();
    private final LocalDate skjæringstidspunkt = IDAG.minusDays(30);

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
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);

        var behandling = scenario.lagre(repositoryProvider);

        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        avsluttBehandlingOgFagsak(behandling);

        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_2() {
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);

        var behandling = scenario.lagre(repositoryProvider);

        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123123123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder)
            .leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        avsluttBehandlingOgFagsak(behandling);

        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_3() {
        var scenario = IAYScenarioBuilder.nyttScenario(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagre(repositoryProvider);

        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        String orgnummer = "123123123";
        var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet(orgnummer));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);

        var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef, virksomhet),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder3 = yrkesBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder3);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        var ref1 = EksternArbeidsforholdRef.ref("ref1");
        var internRef1 = builder.medNyInternArbeidsforholdRef(virksomhet, ref1);
        var yrkesBuilder1 = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef1, virksomhet),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder1.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef1);
        var avtaleBuilder1 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        var avtaleBuilder2 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder1.leggTilAktivitetsAvtale(avtaleBuilder1).leggTilAktivitetsAvtale(avtaleBuilder2);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder1);
        builder.leggTilAktørArbeid(arbeidBuilder);
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

    private String opprettVirksomhet(String orgnummer) {
        return orgnummer;
    }
}
