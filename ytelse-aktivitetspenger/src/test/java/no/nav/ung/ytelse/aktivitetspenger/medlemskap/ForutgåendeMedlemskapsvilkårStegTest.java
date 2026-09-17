package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittUtenlandsopphold;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder.MottattDokumentTestGrunnlag;
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
    private ProsessTriggereRepository prosessTriggereRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ForutgåendeMedlemskapsvilkårSteg steg;
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårTjeneste vilkårTjeneste;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repoProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repoProvider.getVilkårResultatRepository();
        forutgåendeMedlemskapRepository = new OppgittForutgåendeMedlemskapRepository(entityManager);
        mottatteDokumentRepository = new MottatteDokumentRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        manuelleVilkårRekkefølgeTjeneste = new ManuelleVilkårRekkefølgeTjeneste(new BehandlingModellRepository());
        vilkårTjeneste = new VilkårTjeneste(behandlingRepository, perioderTilVurderingTjenester, vilkårResultatRepository);
        steg = new ForutgåendeMedlemskapsvilkårSteg(
            vilkårResultatRepository,
            forutgåendeMedlemskapRepository,
            mottatteDokumentRepository,
            perioderTilVurderingTjenester,
            behandlingRepository,
            manuelleVilkårRekkefølgeTjeneste, vilkårTjeneste);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_ingen_grunnlag_eksisterer() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .medVirkningstidspunkt(FOM)
            .lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_grunnlag_ikke_dekker_hele_forutgående_periode() {
        var forskjøvetFom = FOM.minusWeeks(1);
        var forskjøvetVilkårPeriode = new Periode(forskjøvetFom, TOM);
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, forskjøvetVilkårPeriode)
            .medVirkningstidspunkt(forskjøvetFom)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP, FOM.minusYears(5), FOM.minusDays(1), Set.of()));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_utenlandsopphold_er_ikke_norge() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .medVirkningstidspunkt(FOM)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP, FOM.minusYears(5), FOM.minusDays(1), Set.of(
            new OppgittUtenlandsopphold(LocalDate.of(2020, 1, 1), LocalDate.of(2022, 3, 31), Landkoder.SWE, false,  "ABC123"),
            new OppgittUtenlandsopphold(LocalDate.of(2022, 4, 1), LocalDate.of(2024, 6, 30), Landkoder.USA, true, null)
        )));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_oppfylle_vilkår_for_flere_perioder_med_utenlandsopphold_norge() {
        var periode1 = new Periode(FOM, LocalDate.of(2024, 7, 31));
        var periode2 = new Periode(LocalDate.of(2024, 9, 1), TOM);
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, periode1)
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, periode2)
            .medVirkningstidspunkt(FOM)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP, FOM.minusYears(5), TOM.minusDays(1), Set.of(
            new OppgittUtenlandsopphold(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 9, 29), Landkoder.NOR, false, "ABC123")
        )));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));


        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow();
        assertThat(vilkår.getPerioder()).hasSize(2);
        assertThat(vilkår.getPerioder()).allSatisfy(p -> {
            assertThat(p.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
            assertThat(p.getRegelInput()).contains("NOR");
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
            .medVirkningstidspunkt(FOM)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP, FOM.minusYears(5), TOM.minusDays(1), Set.of()));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

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
        LocalDate andreFom = LocalDate.of(2024, 8, 16);
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, VILKÅR_PERIODE)
            .medVirkningstidspunkt(FOM)
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, andreFom.minusDays(1)))
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_OPPFYLT, new Periode(andreFom, TOM))
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(), nyPeriode(JP, FOM.minusYears(5), FOM.minusDays(1), Set.of()));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow();
        assertThat(vilkår.getPerioder()).hasSize(2);
        var perioderListe = vilkår.getPerioder().stream().sorted(Comparator.comparing(VilkårPeriode::getFom)).toList();
        assertThat(perioderListe.get(0).getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT);
        assertThat(perioderListe.get(1).getGjeldendeUtfall()).isEqualTo(Utfall.IKKE_RELEVANT);
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }

    private static OppgittForutgåendeMedlemskapPeriode nyPeriode(
        JournalpostId journalpostId,
        LocalDate fom,
        LocalDate tom,
        Set<OppgittUtenlandsopphold> utenlandsopphold) {
        return OppgittForutgåendeMedlemskapPeriode.builder()
            .medJournalpostId(journalpostId)
            .medFom(fom)
            .medTom(tom)
            .medUtenlandsopphold(utenlandsopphold)
            .medHarBoddINorge(true)
            .medHarJobbetINorge(null)
            .medHarJobbetUtenforNorge(!utenlandsopphold.isEmpty())
            .build();
    }
}
