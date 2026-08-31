package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.sporing.AvklaringSporing;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.etterlysning.UttalelseData;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BistandsvilkårStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private StartdatoRepository startdatoRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private AvklaringSporing avklaringSporing;
    private BistandsvilkårSteg steg;

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
        avklaringSporing = new AvklaringSporing(new BehandingprosessSporingRepository(entityManager));

        steg = lagSteg(List.of());
    }

    @Test
    void skal_gi_manuelt_aksjonspunkt_nar_det_ikke_finnes_avklaring() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR);
        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.BISTANDSVILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_VURDERT);
    }

    @Test
    void skal_avslå_automatisk_nar_ikke_14a_vedtak_og_bruker_ikke_har_uttalelse() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);

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

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var bistandsvurdering = hentVurderinger(behandling).stream().findFirst().orElseThrow();
        assertThat(bistandsvurdering.getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(bistandsvurdering.getPeriode().getTomDato()).isEqualTo(TOM);
        assertThat(bistandsvurdering.isGodkjent()).isFalse();
        assertThat(bistandsvurdering.getIkkeOppfyltÅrsak()).isEqualTo(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK);

        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.BISTANDSVILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT);
    }

    @Test
    void skal_ikke_avslå_automatisk_nar_bruker_har_uttalelse() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);

        var etterlysningMedUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            LocalDateTime.of(2026, 2, 15, 12, 0),
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(true, "Jeg har vedtak etter § 14 a", new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(etterlysningMedUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_ikke_avslå_automatisk_nar_det_er_valgt_a_ikke_varsle() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, false);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_ikke_avslå_automatisk_for_avkortet_arsak() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, BistandsvilkårIkkeOppfyltÅrsak.AVKORTET, true);

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

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_sette_pa_vent_nar_periode_venter_pa_etterlysning() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        var avklaring = lagreForeslåttAvklaring(behandling.getId(), FOM, TOM, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);

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
            .containsExactly(EtterlysningType.UTTALELSE_BISTAND.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().getFirst().getFrist()).isEqualTo(frist);
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    @Test
    void skal_prioritere_vent_nar_en_periode_er_manuell_og_en_periode_venter_pa_etterlysning() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);
        var avklaring1 = lagAvklaring(FOM, TOM, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);
        var avklaring2 = lagAvklaring(fom2, tom2, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);
        var lagrede = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BISTANDSVILKÅR, Set.of(avklaring1, avklaring2));

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
            .containsExactly(EtterlysningType.UTTALELSE_BISTAND.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktListe())
            .doesNotContain(AksjonspunktDefinisjon.VURDER_BISTANDSVILKÅR);
    }

    @Test
    void skal_utfores_uten_aksjonspunkt_nar_perioden_allerede_er_vurdert_som_oppfylt() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        inngangsvilkårVurderingRepository.lagreBistandsVurderinger(behandling.getId(), List.of(
            new BistandsvilkårResultatPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
                true,
                null,
                true,
                "Vurdert i tidligere behandling",
                null,
                "A12345",
                LocalDateTime.now())
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkår = vilkårResultatRepository.hent(behandling.getId()).getVilkår(VilkårType.BISTANDSVILKÅR).orElseThrow().getPerioder();
        assertThat(vilkår).allMatch(it -> it.getGjeldendeUtfall() == Utfall.OPPFYLT);
    }

    @Test
    void skal_utfores_uten_aksjonspunkt_nar_ingen_perioder_til_vurdering() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(hentVurderinger(behandling)).isEmpty();
    }

    private UUID finnReferanse(Set<VilkårPeriodeAvklaring> lagrede, LocalDate fom) {
        return lagrede.stream()
            .filter(a -> a.getPeriode().getFomDato().equals(fom))
            .findFirst()
            .orElseThrow()
            .getReferanse();
    }

    private Collection<BistandsvilkårResultatPeriode> hentVurderinger(Behandling behandling) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBistandsvilkårResultatPerioder)
            .orElse(List.of());
    }

    private VilkårPeriodeAvklaring lagreForeslåttAvklaring(long behandlingId, LocalDate fom, LocalDate tom, BistandsvilkårIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VilkårType.BISTANDSVILKÅR,
            Set.of(lagAvklaring(fom, tom, årsak, skalSendeVarsel)));
        return lagret.stream().findFirst().orElseThrow();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, BistandsvilkårIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            årsak.getKode(),
            "Begrunnelse for relevante fakta lagt til grunn i avklaring",
            skalSendeVarsel,
            skalSendeVarsel ? "Fritekst til varselet" : null,
            skalSendeVarsel ? null : "Begrunnelse for ikke varsling",
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-vilkår"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode)));
        return behandling;
    }

    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.BISTANDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(fom2, tom2))
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
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode1),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode2)));
        return behandling;
    }

    private BistandsvilkårSteg lagSteg(List<EtterlysningData> etterlysninger) {
        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, vilkårsPerioderTilVurderingTjenester, vilkårResultatRepository);
        var etterlysningTjeneste = new EtterlysningTjeneste(null, null) {
            @Override
            public List<EtterlysningData> hentGjeldendeEtterlysninger(Long behandlingId, Long fagsakId, EtterlysningType type) {
                return etterlysninger;
            }
        };

        return new BistandsvilkårSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            vilkårsPerioderTilVurderingTjenester,
            etterlysningTjeneste,
            vilkårsavklaringGrunnlagRepository,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            avklaringSporing
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
