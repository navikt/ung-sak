package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittBosted;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder.MottattDokumentTestGrunnlag;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ForutgåendeMedlemskapsvilkårStegTest {

    private static final LocalDate FOM = LocalDate.of(2024, 7, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 9, 30);
    private static final Periode VILKÅR_PERIODE = new Periode(FOM, TOM);
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
    void skal_returnere_aksjonspunkt_når_ingen_grunnlag_eksisterer() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .lagre(entityManager);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_grunnlag_ikke_dekker_hele_forutgående_periode() {
        var forskjøvetFom = FOM.minusWeeks(1);
        var forskjøvetVilkårPeriode = new Periode(forskjøvetFom, TOM);
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, forskjøvetVilkårPeriode)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), FOM.minusDays(1), Set.of());

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_ett_bosted_er_utenfor_eøs() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), FOM.minusDays(1), Set.of(
            new OppgittBosted(LocalDate.of(2020, 1, 1), LocalDate.of(2022, 3, 31), "SWE"),
            new OppgittBosted(LocalDate.of(2022, 4, 1), LocalDate.of(2024, 6, 30), "USA")
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_oppfylle_vilkår_for_flere_perioder_med_eøs_bosted() {
        var periode1 = new Periode(FOM, LocalDate.of(2024, 7, 31));
        var periode2 = new Periode(LocalDate.of(2024, 9, 1), TOM);
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, periode1)
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, periode2)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), TOM.minusDays(1), Set.of(
            new OppgittBosted(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 9, 29), "SWE")
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder()).allSatisfy(p -> {
            assertThat(p.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
            assertThat(p.getRegelInput()).contains("SWE");
            assertThat(p.getRegelEvaluering()).contains("OPPFYLT");
        });
    }

    @Test
    void skal_sette_avslått_periode_til_ikke_relevant_og_oppfylle_resten() {
        var periode1 = new Periode(FOM, LocalDate.of(2024, 7, 31));
        var periode2 = new Periode(LocalDate.of(2024, 9, 1), TOM);
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, periode1)
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, periode2)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_OPPFYLT, periode1)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.OPPFYLT, periode2)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), TOM.minusDays(1), Set.of());

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var perioder = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow()
            .getPerioder();
        assertThat(perioder).hasSize(2);
        var perioderListe = perioder.stream().sorted(Comparator.comparing(VilkårPeriode::getFom)).toList();
        assertThat(perioderListe.get(0).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_RELEVANT);
        assertThat(perioderListe.get(1).getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    @Test
    void skal_vurdere_perioden_når_annet_vilkår_er_delvis_avslått_i_samme_periode() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, LocalDate.of(2024, 8, 15)))
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(LocalDate.of(2024, 8, 16), TOM))
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), JP, FOM.minusYears(5), FOM.minusDays(1), Set.of());

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().getFirst().getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}
