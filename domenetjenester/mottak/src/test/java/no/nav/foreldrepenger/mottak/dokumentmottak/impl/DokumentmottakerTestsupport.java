package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static no.nav.k9.kodeverk.behandling.BehandlingType.FØRSTEGANGSSØKNAD;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
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

    protected Behandling opprettBehandling(FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType, BehandlingResultatType behandlingResultatType,
                                           Avslagsårsak avslagsårsak, VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {
        var scenario = TestScenarioBuilder.builderMedSøknad(fagsakYtelseType)
            .medBehandlingType(behandlingType);
        return opprettBehandling(scenario, behandlingResultatType, avslagsårsak, vedtakResultatType, vedtaksdato);
    }

    private Behandling opprettBehandling(AbstractTestScenario<?> scenario, BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak,
                                         VedtakResultatType vedtakResultatType, LocalDate vedtaksdato) {

        scenario.medBehandlingsresultat(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingLås behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, behandlingLås);

        final var behandlingsresultat = behandling.getBehandlingsresultat();
        BehandlingVedtak originalVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(vedtaksdato.atStartOfDay())
            .medBehandlingsresultat(behandlingsresultat)
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
        return DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, "123");
    }

    protected MottattDokument dummyVedleggDokument(Behandling behandling) {
        DokumentTypeId dokumentTypeId = DokumentTypeId.LEGEERKLÆRING;
        return DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, "456");
    }

    protected MottattDokument dummySøknadDokument(Behandling behandling) {
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
        return DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, "456");
    }

}
