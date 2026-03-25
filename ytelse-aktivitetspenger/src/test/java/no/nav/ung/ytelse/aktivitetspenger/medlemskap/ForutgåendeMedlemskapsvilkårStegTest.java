package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.Versjon;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.Landkode;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Aktivitetspenger;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.aktivitetspenger.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ForutgåendeMedlemskapsvilkårStegTest {

    private static final LocalDate FOM = LocalDate.of(2024, 7, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 9, 30);
    private static final Periode SØKNAD_PERIODE = new Periode(FOM, TOM);
    private static final no.nav.ung.sak.typer.Periode VILKÅR_PERIODE = new no.nav.ung.sak.typer.Periode(FOM, TOM);

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private ForutgåendeMedlemskapsvilkårSteg steg;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repoProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repoProvider.getVilkårResultatRepository();
        steg = new ForutgåendeMedlemskapsvilkårSteg(
            repoProvider.getVilkårResultatRepository(),
            new ForutgåendeMedlemskapTjeneste(
                new MottatteDokumentRepository(entityManager),
                new no.nav.ung.sak.mottak.dokumentmottak.SøknadParser()
            )
        );
    }

    @Test
    void skal_returnere_uten_aksjonspunkter_når_vilkår_allerede_avklart() {
        var behandling = lagScenario(Utfall.IKKE_OPPFYLT, null);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_returnere_aksjonspunkt_når_ingen_søknad_funnet() {
        var behandling = lagScenario(Utfall.IKKE_VURDERT, null);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_uten_aksjonspunkter_når_ingen_bosted_oppgitt() {
        var behandling = lagScenario(Utfall.IKKE_VURDERT, lagSøknadPayload(Map.of()));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertVilkårOppfylt(behandling);
    }

    @Test
    void skal_returnere_uten_aksjonspunkter_når_bosted_er_eøs_land() {
        var behandling = lagScenario(Utfall.IKKE_VURDERT, lagSøknadPayload(Map.of(
            new Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
            Landkode.SVERIGE
        )));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertVilkårOppfylt(behandling);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_bosted_er_utenfor_eøs() {
        var resultat = utførStegMedBosted(Map.of(
            new Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
            Landkode.USA
        ));

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    @Test
    void skal_returnere_aksjonspunkt_når_ett_bosted_er_utenfor_eøs() {
        var resultat = utførStegMedBosted(Map.of(
            new Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31)),
            Landkode.SVERIGE,
            new Periode(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 6, 30)),
            Landkode.USA
        ));

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
    }

    private void assertVilkårOppfylt(Behandling behandling) {
        var vilkår = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET)
            .orElseThrow();
        assertThat(vilkår.getPerioder()).allSatisfy(p ->
            assertThat(p.getGjeldendeUtfall()).isEqualTo(Utfall.OPPFYLT));
    }

    private BehandleStegResultat utførStegMedBosted(Map<Periode, Landkode> bostederMap) {
        var payload = lagSøknadPayload(bostederMap);
        var behandling = lagScenario(Utfall.IKKE_VURDERT, payload);
        return utførSteg(behandling);
    }

    private Behandling lagScenario(Utfall utfall, String søknadPayload) {
        var scenario = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, utfall, VILKÅR_PERIODE);

        if (søknadPayload != null) {
            scenario.medMottattDokument(new AktivitetspengerTestScenarioBuilder.MottattDokumentTestGrunnlag(Brevkode.AKTIVITETSPENGER_SOKNAD, søknadPayload));
        }

        return scenario.lagre(entityManager);
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }

    private static String lagSøknadPayload(Map<Periode, Landkode> bostederMap) {
        Map<Periode, Bosteder.BostedPeriodeInfo> perioder = new java.util.LinkedHashMap<>();
        bostederMap.forEach((periode, land) -> perioder.put(periode, new Bosteder.BostedPeriodeInfo().medLand(land)));

        var aktivitetspenger = new Aktivitetspenger()
            .medSøknadsperiode(SØKNAD_PERIODE)
            .medForutgåendeBosteder(new Bosteder().medPerioder(perioder));
        var søknad = new Søknad(
            new SøknadId("test-søknad"),
            new Versjon("1.0.0"),
            ZonedDateTime.now(),
            new Søker(NorskIdentitetsnummer.of("12345678901")),
            aktivitetspenger);
        return JsonUtils.toString(søknad);
    }
}
