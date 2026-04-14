package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittBosted;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ForutgåendeMedlemskapsvilkårStegTest {

    private static final LocalDate FOM = LocalDate.of(2024, 7, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 9, 30);
    private static final no.nav.ung.sak.typer.Periode VILKÅR_PERIODE = new no.nav.ung.sak.typer.Periode(FOM, TOM);
    private static final JournalpostId JP = new JournalpostId("JP1");

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ForutgåendeMedlemskapsvilkårSteg steg;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repoProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repoProvider.getVilkårResultatRepository();
        forutgåendeMedlemskapRepository = new OppgittForutgåendeMedlemskapRepository(entityManager);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        steg = new ForutgåendeMedlemskapsvilkårSteg(
            vilkårResultatRepository,
            forutgåendeMedlemskapRepository,
            mottatteDokumentRepository,
            perioderTilVurderingTjenester,
            behandlingRepository
        );
    }

    @Test
    void skal_returnere_uten_aksjonspunkter_når_vilkår_allerede_avklart() {
        var behandling = lagScenario(Utfall.IKKE_OPPFYLT);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_returnere_aksjonspunkt_når_ingen_grunnlag_eksisterer() {
        var behandling = lagScenario(Utfall.IKKE_VURDERT);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_uten_aksjonspunkter_når_ingen_bosted_oppgitt() {
        var behandling = lagScenario(Utfall.IKKE_VURDERT);
        lagreMottattDokument(behandling, JP, LocalDateTime.now());
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), FOM.minusDays(1), Set.of());

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertVilkårOppfylt(behandling);
    }

    @Test
    void skal_returnere_uten_aksjonspunkter_når_bosted_er_eøs_land() {
        var behandling = lagScenario(Utfall.IKKE_VURDERT);
        lagreMottattDokument(behandling, JP, LocalDateTime.now());
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), FOM.minusDays(1), Set.of(
            new OppgittBosted(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 6, 30), "SWE")
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertVilkårOppfylt(behandling);

        var periode = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow()
            .getPerioder().iterator().next();
        assertThat(periode.getRegelInput()).contains("SWE");
        assertThat(periode.getRegelEvaluering()).contains("OPPFYLT");
    }

    @Test
    void skal_returnere_aksjonspunkt_når_grunnlag_ikke_dekker_hele_forutgående_periode() {
        var forskjøvetFom = FOM.minusWeeks(1);
        var forskjøvetVilkårPeriode = new no.nav.ung.sak.typer.Periode(forskjøvetFom, TOM);
        var behandling = lagScenario(Utfall.IKKE_VURDERT, forskjøvetVilkårPeriode);
        lagreMottattDokument(behandling, JP, LocalDateTime.now());
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), FOM.minusDays(1), Set.of());

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_ett_bosted_er_utenfor_eøs() {
        var behandling = lagScenario(Utfall.IKKE_VURDERT);
        lagreMottattDokument(behandling, JP, LocalDateTime.now());
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), FOM.minusDays(1), Set.of(
            new OppgittBosted(LocalDate.of(2020, 1, 1), LocalDate.of(2022, 3, 31), "SWE"),
            new OppgittBosted(LocalDate.of(2022, 4, 1), LocalDate.of(2024, 6, 30), "USA")
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    private void assertVilkårOppfylt(Behandling behandling) {
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow();
        assertThat(vilkår.getPerioder()).allSatisfy(p ->
            assertThat(p.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT));
    }

    private Behandling lagScenario(Utfall utfall) {
        return lagScenario(utfall, VILKÅR_PERIODE);
    }

    private Behandling lagScenario(Utfall utfall, no.nav.ung.sak.typer.Periode vilkårPeriode) {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, utfall, vilkårPeriode)
            .lagre(entityManager);
    }

    private void lagreMottattDokument(Behandling behandling, JournalpostId journalpostId, LocalDateTime mottattTidspunkt) {
        var dokument = new MottattDokument.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medJournalPostId(journalpostId)
            .medMottattTidspunkt(mottattTidspunkt)
            .build();
        mottatteDokumentRepository.lagre(dokument, DokumentStatus.GYLDIG);
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}
