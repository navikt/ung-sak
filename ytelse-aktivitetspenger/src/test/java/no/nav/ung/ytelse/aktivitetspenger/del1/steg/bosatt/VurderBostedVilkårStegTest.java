package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.sporing.BehandingprosessSporingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatHolder;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderBostedVilkårStegTest {

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
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private StartdatoRepository startdatoRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private VurderBostedVilkårSteg steg;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;
    private BehandingprosessSporingRepository behandlingprosessSporingRepository;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);
        startdatoRepository = new StartdatoRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, vilkårResultatRepository);
        behandlingprosessSporingRepository = new BehandingprosessSporingRepository(entityManager);

        steg = lagSteg(List.of());
    }

    @Test
    void skal_passere_uten_aksjonspunkt_og_uten_opphorsresultat_nar_bruker_er_bosatt_hele_perioden() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(
            lagBostedsPeriodeAvklaringErBosatt(FOM, TOM)
        ));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_opprette_vilkårvurderingresultat_ved_fraflytting_automatisk() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, false, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(avklaring));

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var vilkårVurderingResultat = inngangsvilkårVurderingRepository.hentGrunnlag(behandling.getId());
        var bostedsvurdering = vilkårVurderingResultat.flatMap(AktivitetspengerInngangsvilkårResultatGrunnlag::getBostedsvilkårResultatHolder).map(BostedsvilkårResultatHolder::getVurderinger).map(Collection::stream).orElseThrow().findFirst().orElseThrow();
        assertThat(bostedsvurdering.getPeriode().getFomDato()).isEqualTo(FOM);
        assertThat(bostedsvurdering.getPeriode().getTomDato()).isEqualTo(TOM);
        assertThat(bostedsvurdering.getIkkeOppfyltÅrsak()).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);
    }

    @Test
    void skal_vilkårvurdere_manuelt_når_det_ikke_varsles() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);

        var avklaring = lagBostedsPeriodeAvklaring(FOM, TOM, false, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, false);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(avklaring));

        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            avklaring.getReferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0),
            new UttalelseData(false, null, new JournalpostId("jp-uttalelse-1"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning));
        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);
    }

    @Test
    void skal_sette_pa_vent_nar_periode_venter_pa_etterlysning() {
        var behandling = opprettBehandlingMedVilkårOgPeriode();
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(
            lagBostedsPeriodeAvklaring(FOM, TOM, true, null, false)
        ));
        var frist = LocalDateTime.of(2026, 2, 15, 12, 0);
        var ventendeEtterlysning = EtterlysningData.utenUttalelse(
            EtterlysningStatus.VENTER,
            frist,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        steg = lagSteg(List.of(ventendeEtterlysning));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktResultater()).hasSize(1);
        assertThat(resultat.getAksjonspunktResultater().getFirst().getFrist()).isEqualTo(frist);
    }

    @Test
    void skal_prioritere_vent_nar_en_periode_er_manuell_og_en_periode_venter_pa_etterlysning() {
        var fom2 = TOM.plusDays(1);
        var tom2 = fom2.plusDays(30);
        var behandling = opprettBehandlingMedToVilkårsperioder(fom2, tom2);
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-søknad-1", FOM, true);
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), List.of(
            lagBostedsPeriodeAvklaringErBosatt(FOM, TOM),
            lagBostedsPeriodeAvklaringErBosatt(fom2, tom2)
        ));
        var frist = LocalDateTime.of(2026, 3, 1, 10, 0);
        var ventendeEtterlysning = EtterlysningData.utenUttalelse(
            EtterlysningStatus.VENTER,
            frist,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            LocalDateTime.of(2026, 1, 10, 9, 0)
        );
        var mottattSvarMedUttalelse = new EtterlysningData(
            EtterlysningStatus.MOTTATT_SVAR,
            frist,
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2),
            LocalDateTime.of(2026, 2, 10, 9, 0),
            new UttalelseData(true, "flyttet", new JournalpostId("jp-svar"))
        );
        steg = lagSteg(List.of(ventendeEtterlysning, mottattSvarMedUttalelse));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(EtterlysningType.UTTALELSE_BOSTED.tilAutopunktDefinisjon());
        assertThat(resultat.getAksjonspunktListe())
            .doesNotContain(AksjonspunktDefinisjon.VURDER_BOSTEDVILKÅR);
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.SØKNADSFRIST, Utfall.OPPFYLT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-vilkår"));
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));
        prosessTriggereRepository.leggTil(behandling.getId(), java.util.Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode)));
        return behandling;
    }

    private Behandling opprettBehandlingMedToVilkårsperioder(LocalDate fom2, LocalDate tom2) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .leggTilVilkår(VilkårType.BOSTEDSVILKÅR, Utfall.IKKE_VURDERT, new Periode(fom2, tom2))
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
        prosessTriggereRepository.leggTil(behandling.getId(), java.util.Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode1),
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode2)));
        return behandling;
    }

    private VurderBostedVilkårSteg lagSteg(List<EtterlysningData> etterlysninger) {
        var vilkårTjeneste = new VilkårTjeneste(behandlingRepository, vilkårsPerioderTilVurderingTjenester, vilkårResultatRepository);
        var etterlysningTjeneste = new EtterlysningTjeneste(null, null) {
            @Override
            public List<EtterlysningData> hentGjeldendeEtterlysninger(Long behandlingId, Long fagsakId, EtterlysningType type) {
                return etterlysninger;
            }
        };

        return new VurderBostedVilkårSteg(
            manuelleVilkårRekkefølgeTjeneste,
            vilkårResultatRepository,
            vilkårTjeneste,
            behandlingRepository,
            bostedsGrunnlagRepository,
            vilkårsPerioderTilVurderingTjenester,
            etterlysningTjeneste,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            behandlingprosessSporingRepository
        );
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }

    private BostedsPeriodeAvklaring lagBostedsPeriodeAvklaringErBosatt(LocalDate fom, LocalDate tom) {
        return lagBostedsPeriodeAvklaring(fom, tom, true, null, false);
    }

    private BostedsPeriodeAvklaring lagBostedsPeriodeAvklaring(LocalDate fom, LocalDate tom, boolean bosatt,
                                                               BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
                                                               boolean skalSendeVarsel) {
        return new BostedsPeriodeAvklaring(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            bosatt,
            ikkeOppfyltÅrsak,
            "Begrunnelse for relevante fakta lagt til grunn i avklaring",
            skalSendeVarsel,
            skalSendeVarsel && BostedsvilkårIkkeOppfyltÅrsak.ANNET.equals(ikkeOppfyltÅrsak) ? "Fritekst til varselet" : null,
            skalSendeVarsel ? null : "Fritekst for ikke varsling",
            "A12345",
            LocalDateTime.now()
        );
    }
}

