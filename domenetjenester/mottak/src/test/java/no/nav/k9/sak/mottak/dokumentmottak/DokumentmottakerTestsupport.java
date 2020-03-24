package no.nav.k9.sak.mottak.dokumentmottak;

import static java.time.LocalDate.now;
import static no.nav.k9.kodeverk.behandling.BehandlingType.FØRSTEGANGSSØKNAD;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.Behandlingsoppretter;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public abstract class DokumentmottakerTestsupport {

    protected static final int FRIST_INNSENDING_UKER = 6;
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    protected final LocalDate DATO_ETTER_INNSENDINGSFRISTEN = LocalDate.now().minusWeeks(FRIST_INNSENDING_UKER + 2);
    protected final LocalDate DATO_FØR_INNSENDINGSFRISTEN = LocalDate.now().minusWeeks(FRIST_INNSENDING_UKER - 2);
    @Inject
    protected DokumentmottakerFelles dokumentmottakerFelles;
    @Inject
    protected MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Inject
    protected Behandlingsoppretter behandlingsoppretter;
    @Inject
    protected Kompletthetskontroller kompletthetskontroller;
    @Inject
    protected BehandlingRepositoryProvider repositoryProvider;

    protected Behandling opprettNyBehandlingUtenVedtak(FagsakYtelseType fagsakYtelseType) {
        Behandling behandling;
        var scenario = TestScenarioBuilder.builderMedSøknad(fagsakYtelseType)
            .medBehandlingType(FØRSTEGANGSSØKNAD);
        behandling = scenario.lagre(repositoryProvider);
        return behandling;
    }

    protected Behandling opprettBehandling(FagsakYtelseType fagsakYtelseType, 
                                           BehandlingType behandlingType, 
                                           BehandlingResultatType behandlingResultatType,
                                           VedtakResultatType vedtakResultatType, 
                                           LocalDate vedtaksdato) {
        var scenario = TestScenarioBuilder.builderMedSøknad(fagsakYtelseType)
            .medBehandlingType(behandlingType);
        return opprettBehandling(scenario, behandlingResultatType, vedtakResultatType, vedtaksdato);
    }

    private Behandling opprettBehandling(AbstractTestScenario<?> scenario, 
                                         BehandlingResultatType behandlingResultatType, 
                                         VedtakResultatType vedtakResultatType,
                                         LocalDate vedtaksdato) {

        scenario.medBehandlingsresultat(behandlingResultatType);
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingLås behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, behandlingLås);

        BehandlingVedtak originalVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(vedtaksdato.atStartOfDay())
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler("fornavn etternavn")
            .build();

        behandling.getFagsak().setAvsluttet();
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingVedtakRepository().lagre(originalVedtak, behandlingLås);

        Vilkårene vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fraOgMed(LocalDate.now())), VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .build();
        repositoryProvider.getBehandlingRepository().lagre(behandling, behandlingLås);
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), vilkårene);

        return behandling;
    }

    protected MottattDokument dummyInntektsmeldingDokument(Behandling behandling) {
        DokumentTypeId dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        return DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "<"+dokumentTypeId+">", now(), "123", DokumentTypeId.INNTEKTSMELDING);
    }


}
