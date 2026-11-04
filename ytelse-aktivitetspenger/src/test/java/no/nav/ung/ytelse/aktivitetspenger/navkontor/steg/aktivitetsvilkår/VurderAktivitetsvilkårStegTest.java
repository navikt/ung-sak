package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import com.fasterxml.jackson.databind.JsonNode;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.sporing.Vilkårsutfallsporing;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.UttalelseData;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall;
import no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfallUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderAktivitetsvilkårStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    @Inject
    private EntityManager entityManager;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    private AktivitetspengerVilkårsPerioderTilVurderingTjeneste aktivitetspengerVilkårsPerioderTilVurderingTjeneste;

    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;

    @Inject
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private StartdatoRepository startdatoRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private Vilkårsutfallsporing vilkårsutfallsporing;
    private VurderAktivitetsvilkårSteg steg;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        startdatoRepository = new StartdatoRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
        vilkårsutfallsporing = new Vilkårsutfallsporing(new BehandingprosessSporingRepository(entityManager));

        steg = lagSteg(List.of());
    }

    @Test
    void skal_sette_ikke_relevant_automatisk_i_førstegangsbehandling() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.AKTIVITETSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);
        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-vilkår"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.AKTIVITETSVILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_RELEVANT);
    }

    @Test
    void skal_gi_manuelt_aksjonspunkt_nar_det_ikke_finnes_avklaring() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR);
        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.AKTIVITETSVILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_vurdere_manuelt_etter_avklaring_fordi_arsak_krever_fritekst() {
        var behandling = opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.ENDRET_AKTIVITET);
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true);

        var etterlysningUtenUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(etterlysningUtenUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR_OPPHØR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_ikke_avslå_automatisk_nar_bruker_har_uttalelse() {
        var behandling = opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.ENDRET_AKTIVITET);
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true);

        var etterlysningMedUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(true, "Jeg deltar i aktiviteten", new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(etterlysningMedUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR_OPPHØR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_ikke_avslå_automatisk_nar_det_er_valgt_a_ikke_varsle() {
        var behandling = opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.ENDRET_AKTIVITET);
        lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, false);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR_OPPHØR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_sette_pa_vent_nar_periode_venter_pa_etterlysning() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true);

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = EtterlysningData.utenUttalelse(
            EtterlysningStatus.VENTER,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        steg = lagSteg(List.of(ventendeEtterlysning));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(EtterlysningType.UTTALELSE_AKTIVITET.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().getFirst().getFrist()).isEqualTo(frist);
        assertThat(hentVurderinger(behandling)).isEmpty();

        var vilkårsperiode = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.AKTIVITETSVILKÅR).orElseThrow().getPerioder().getFirst();
        assertThat(vilkårsperiode.getRegelInput()).isNull();
    }

    @Test
    void skal_prioritere_vent_nar_en_periode_er_manuell_og_en_periode_venter_pa_etterlysning() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);
        var avklaring1 = lagAvklaring(FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true);
        var avklaring2 = lagAvklaring(fom2, tom2, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true);
        var lagrede = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.AKTIVITETSVILKÅR, Set.of(avklaring1, avklaring2));

        var referanse1 = finnReferanse(lagrede, FOM);
        var referanse2 = finnReferanse(lagrede, fom2);

        var frist = LocalDateTime.of(2026, 3, 1, 10, 0);
        var ventendeEtterlysning = EtterlysningData.utenUttalelse(
            EtterlysningStatus.VENTER,
            frist,
            referanse1,
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        var mottattSvarMedUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            referanse2,
            DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2),
            LocalDateTime.of(2026, 2, 10, 9, 0),
            new UttalelseData(true, "uenig", new JournalpostId("jp-svar"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning, mottattSvarMedUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(EtterlysningType.UTTALELSE_AKTIVITET.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktListe())
            .doesNotContain(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR, AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR_OPPHØR);
    }

    @Test
    void skal_ikke_utlede_utfall_fra_tidligere_vilkårsvurdering_uten_avklaring() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        inngangsvilkårVurderingRepository.lagreAktivitetVurderinger(behandling.getId(), List.of(
            new AktivitetsvilkårResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
                true,
                null,
                true,
                "Vurdert i tidligere behandling",
                null,
                "A12345",
                LocalDateTime.now(), null)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .as("uten foreslått avklaring eller etterlysning skal perioden vurderes manuelt, ikke arve utfall fra tidligere vurdering (jf. #1566)")
            .containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR);
        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.AKTIVITETSVILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_VURDERT);
    }

    private VilkårPeriodeAvklaring hentEnesteAvklaring(long behandlingId) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.AKTIVITETSVILKÅR)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .findFirst()
            .orElseThrow();
    }

    @Test
    void skal_utfores_uten_aksjonspunkt_nar_ingen_perioder_til_vurdering() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .leggTilVilkår(VilkårType.AKTIVITETSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_gi_opphørsaksjonspunkt_nar_hele_den_manuelle_tidslinjen_har_foreslatt_avklaring() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.ENDRET_AKTIVITET, BehandlingÅrsakType.ENDRET_AKTIVITET);
        vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.AKTIVITETSVILKÅR, Set.of(
            lagAvklaring(FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, false),
            lagAvklaring(fom2, tom2, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, false)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR_OPPHØR);
    }

    @Test
    void skal_gi_avslagsaksjonspunkt_nar_ingen_manuell_periode_har_foreslatt_avklaring() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .as("saksbehandler kan ha avklart fakta uten å foreslå en vurdering; da må avslagsårsaken oppgis i 5143")
            .containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR);
    }

    @Test
    void skal_feile_nar_bade_avklart_periode_og_ny_soknadsperiode_vurderes_manuelt() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.ENDRET_AKTIVITET, BehandlingÅrsakType.NY_SØKT_PERIODE);
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true);
        steg = lagSteg(List.of(mottattUttalelse(avklaring.getReferanse())));

        assertThatThrownBy(() -> utførSteg(behandling))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void skal_ikke_vurdere_periode_uten_avklaring_nar_bare_avklaringsarsaken_er_trigger() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.ENDRET_AKTIVITET, BehandlingÅrsakType.ENDRET_AKTIVITET);
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true);

        steg = lagSteg(List.of(new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1")))));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_AKTIVITETSVILKÅR_OPPHØR);
    }

    private UUID finnReferanse(Set<VilkårPeriodeAvklaring> lagrede, LocalDate fom) {
        return lagrede.stream()
            .filter(a -> a.getPeriode().getFomDato().equals(fom))
            .findFirst()
            .orElseThrow()
            .getReferanse();
    }

    private Collection<AktivitetsvilkårResultatPeriode> hentVurderinger(Behandling behandling) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentAktivitetsvilkårResultatPerioder)
            .orElse(Set.of());
    }

    private VilkårPeriodeAvklaring lagreForeslåttAvklaring(long behandlingId, LocalDate fom, LocalDate tom, AktivitetsvilkåretIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VilkårType.AKTIVITETSVILKÅR,
            Set.of(lagAvklaring(fom, tom, årsak, skalSendeVarsel)));
        return lagret.stream().findFirst().orElseThrow();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, AktivitetsvilkåretIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            årsak.getKode(),
            "Begrunnelse for relevante fakta lagt til grunn i avklaring",
            skalSendeVarsel,
            skalSendeVarsel ? "Fritekst til varselet" : null,
            skalSendeVarsel ? null : "Begrunnelse for ikke varsling",
            AktivitetsavklaringKildeType.BRUKER,
            null,
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        return opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType.NY_SØKT_PERIODE);
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode(BehandlingÅrsakType årsak) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .leggTilVilkår(VilkårType.AKTIVITETSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-vilkår"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(årsak, periode)));
        return behandling;
    }

    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2) {
        return opprettBehandlingMedToVilkårsperioder(fom2, tom2, BehandlingÅrsakType.NY_SØKT_PERIODE, BehandlingÅrsakType.NY_SØKT_PERIODE);
    }

    // Årsak null gir perioden ingen prosesstrigger.
    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2, BehandlingÅrsakType årsakPeriode1, BehandlingÅrsakType årsakPeriode2) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .leggTilVilkår(VilkårType.AKTIVITETSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.AKTIVITETSVILKÅR, Utfall.IKKE_VURDERT, new Periode(fom2, tom2))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(fom2, tom2))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(fom2, tom2))
            .lagre(entityManager);

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2);
        var søktStartdato1 = new SøktStartdato(FOM, new JournalpostId("jp-vilkår-1"));
        var søktStartdato2 = new SøktStartdato(fom2, new JournalpostId("jp-vilkår-2"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato1, søktStartdato2));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato1, søktStartdato2)));
        var triggere = new HashSet<Trigger>();
        if (årsakPeriode1 != null) {
            triggere.add(new Trigger(årsakPeriode1, periode1));
        }
        if (årsakPeriode2 != null) {
            triggere.add(new Trigger(årsakPeriode2, periode2));
        }
        prosessTriggereRepository.leggTil(behandling.getId(), triggere);
        return behandling;
    }

    private static EtterlysningData mottattUttalelse(UUID avklaringReferanse) {
        return new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaringReferanse,
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(true, "Jeg deltar fortsatt i aktiviteten", new JournalpostId("jp-uttalelse-1")));
    }

    private VurderAktivitetsvilkårSteg lagSteg(List<EtterlysningData> etterlysninger) {
        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, vilkårsPerioderTilVurderingTjenester, vilkårResultatRepository);
        var etterlysningTjeneste = new EtterlysningTjeneste(null, null) {
            @Override
            public List<EtterlysningData> hentGjeldendeEtterlysninger(Long behandlingId, Long fagsakId, EtterlysningType type) {
                return etterlysninger;
            }
        };

        return new VurderAktivitetsvilkårSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            vilkårsPerioderTilVurderingTjenester,
            etterlysningTjeneste,
            vilkårsavklaringGrunnlagRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            vilkårsutfallsporing,
            vilkårsvurderingHistorikkinnslagTjeneste,
            aktivitetspengerVilkårsPerioderTilVurderingTjeneste
        );
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}
