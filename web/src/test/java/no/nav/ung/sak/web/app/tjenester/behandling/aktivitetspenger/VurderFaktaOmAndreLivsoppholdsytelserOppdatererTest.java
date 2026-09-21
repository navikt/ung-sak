package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.VilkårsavklaringEtterlysningTjeneste;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.AndreLivsoppholdsytelserAvklaringIkkeOppfyltDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.AndreLivsoppholdsytelserFaktaavklaringPeriodeDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.livsopphold.VurderFaktaOmAndreLivsoppholdsytelserDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser.AndreLivsoppholdsytelserAvklaringTjeneste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderFaktaOmAndreLivsoppholdsytelserOppdatererTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode PERIODE_2 = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    private static final VilkårType VILKÅR = VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
    private static final AndreLivsoppholdsytelserIkkeOppfyltÅrsak ÅRSAK = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER;
    private static final String SAKSBEHANDLER = "saksbehandler1";
    private static final String BEGRUNNELSE_IKKE_VARSEL = "Begrunnelse for hvorfor det ikke varsles";

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private VurderFaktaOmAndreLivsoppholdsytelserOppdaterer oppdaterer;
    private Behandling behandling;
    private VilkårResultatRepository vilkårResultatRepository;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(SAKSBEHANDLER);
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        var behandlingRepository = new BehandlingRepository(entityManager);
        var historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        etterlysningRepository = new EtterlysningRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
        var avklaringTjeneste = new AndreLivsoppholdsytelserAvklaringTjeneste(
            vilkårsavklaringGrunnlagRepository,
            inngangsvilkårVurderingTjeneste,
            vilkårResultatRepository);

        oppdaterer = new VurderFaktaOmAndreLivsoppholdsytelserOppdaterer(
            behandlingRepository,
            historikkinnslagRepository,
            new VilkårsavklaringEtterlysningTjeneste(etterlysningRepository, prosessTaskTjeneste),
            vilkårsPerioderTilVurderingTjenester,
            avklaringTjeneste,
            inngangsvilkårVurderingTjeneste
        );

        behandling = opprettBehandlingMedVilkårOgPeriode();
    }

    @Test
    void skal_lagre_foreslatt_avklaring_og_opprette_etterlysning() {
        oppdater(dtoMedVarsel(new ÅpenPeriode(FOM, TOM), ÅRSAK));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(avklaringer.getFirst().getIkkeOppfyltÅrsakKode()).isEqualTo(ÅRSAK.getKode());
        assertThat(avklaringer.getFirst().getKildeKode()).isEqualTo(AndreLivsoppholdsytelserAvklaringKildeType.NAV.getKode());
        assertThat(avklaringer.getFirst().getVurdertAv()).isEqualTo(SAKSBEHANDLER);
        assertThat(avklaringer.getFirst().getAvklaringtype()).isEqualTo(Avklaringtype.AVSLAG);

        var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_ANDRE_LIVSOPPHOLDSYTELSER);
        assertThat(etterlysninger).hasSize(1);
        assertThat(etterlysninger.getFirst().getGrunnlagsreferanse()).isEqualTo(avklaringer.getFirst().getReferanse());
        verify(prosessTaskTjeneste).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_avklaring_er_uendret() {
        var dto = dtoMedVarsel(new ÅpenPeriode(FOM, TOM), ÅRSAK);
        oppdater(dto);

        var referanseFørstegang = hentSorterteAvklaringer().getFirst().getReferanse();

        oppdater(dto);

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId())).isEmpty();
        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_ANDRE_LIVSOPPHOLDSYTELSER))
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .containsExactly(referanseFørstegang);
        verify(prosessTaskTjeneste, times(1)).lagre(any(ProsessTaskData.class));
        assertEtterlysningerPekerPåAvklaringIAktivtGrunnlag();
    }

    @Test
    void skal_ikke_opprette_eller_avbryte_nar_kun_begrunnelse_er_endret() {
        oppdater(dtoMedVarsel(new ÅpenPeriode(FOM, TOM), ÅRSAK, "opprinnelig begrunnelse"));

        var referanseFørstegang = hentSorterteAvklaringer().getFirst().getReferanse();

        oppdater(dtoMedVarsel(new ÅpenPeriode(FOM, TOM), ÅRSAK, "rettet begrunnelse"));

        assertThat(hentSorterteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getBegrunnelse)
            .as("den rettede begrunnelsen skal lagres")
            .containsExactly("rettet begrunnelse");

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId())).isEmpty();
        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_ANDRE_LIVSOPPHOLDSYTELSER))
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .as("begrunnelsen vises ikke for bruker, så varselet skal ikke sendes på nytt")
            .containsExactly(referanseFørstegang);
        verify(prosessTaskTjeneste, times(1)).lagre(any(ProsessTaskData.class));
        assertEtterlysningerPekerPåAvklaringIAktivtGrunnlag();
    }

    @Test
    void endret_ytelse_i_varselet_skal_gi_ny_etterlysning() {
        oppdater(dtoMedVarsel(new ÅpenPeriode(FOM, TOM), ÅRSAK));
        var referanseFørstegang = hentSorterteAvklaringer().getFirst().getReferanse();
        verify(prosessTaskTjeneste, times(1)).lagre(any(ProsessTaskData.class));
        clearInvocations(prosessTaskTjeneste);

        oppdater(dtoMedVarsel(new ÅpenPeriode(FOM, TOM), AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_UFØRETRYGD));

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId()))
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .containsExactly(referanseFørstegang);
        assertThat(hentSorterteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .doesNotContain(referanseFørstegang);
        // avbryte etterlysningen knyttet til den gamle avklaringen, og opprette etterlysning for den nye
        verify(prosessTaskTjeneste, times(2)).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_lagre_avklaring_uten_varsel_uten_a_opprette_etterlysning() {
        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), ÅRSAK));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().skalSendeVarsel()).isFalse();
        assertThat(avklaringer.getFirst().getBegrunnelseIkkeVarsel()).isEqualTo(BEGRUNNELSE_IKKE_VARSEL);

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId())).isEmpty();
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void apen_tom_skal_lukkes_mot_maksdato_i_vilkarsperioden_og_markeres_som_opphor() {
        oppdater(dtoUtenVarsel(new ÅpenPeriode(FOM.plusMonths(1), null), ÅRSAK));

        var avklaringer = hentSorterteAvklaringer();
        assertThat(avklaringer).hasSize(1);
        assertThat(avklaringer.getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1), TOM));
        assertThat(avklaringer.getFirst().getAvklaringtype())
            .as("perioden lukkes mot maksdato, så det er avklaringtypen - ikke en åpen tom - som skiller opphør fra avslag")
            .isEqualTo(Avklaringtype.OPPHØR);
    }

    @Test
    void skal_sette_vilkarsperiode_for_avklaringen_til_ikke_vurdert() {
        var dto = dtoUtenVarsel(new ÅpenPeriode(FOM, TOM), ÅRSAK);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);

        oppdaterer.oppdater(dto, param);

        var vilkår = param.getVilkårResultatBuilder().build().getVilkår(VILKÅR).orElseThrow();
        assertThat(vilkår.getPerioder()).hasSize(1);
        assertThat(vilkår.getPerioder().getFirst().getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(vilkår.getPerioder().getFirst().getUtfall()).isEqualTo(Utfall.IKKE_VURDERT);
    }

    private void oppdater(VurderFaktaOmAndreLivsoppholdsytelserDto dto) {
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
    }

    private Behandling opprettBehandlingMedVilkårOgPeriode() {
        var behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.AKTIVITETSPENGER)
            .leggTilVilkår(VILKÅR, Utfall.IKKE_VURDERT, new Periode(FOM, TOM))
            .lagre(entityManager);

        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-søknad-1"));
        var startdatoRepository = new StartdatoRepository(entityManager);
        startdatoRepository.lagre(behandling.getId(), List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(søktStartdato)));

        new ProsessTriggereRepository(entityManager).leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        // Aksjonspunktet forutsetter at inngangsvilkår-vurderingsgrunnlaget allerede er opprettet (jf. faktaavklaringssteget)
        inngangsvilkårVurderingRepository.lagreYtelseVurderinger(behandling.getId(), List.of());

        return behandling;
    }

    /**
     * En etterlysning som beholdes må peke på en avklaring i det aktive grunnlaget — ellers finner
     * oppgaveoppretteren ikke igjen avklaringen når varselet skal sendes.
     */
    private void assertEtterlysningerPekerPåAvklaringIAktivtGrunnlag() {
        var referanserIAktivtGrunnlag = hentSorterteAvklaringer().stream()
            .map(VilkårPeriodeAvklaring::getReferanse)
            .toList();

        assertThat(etterlysningRepository.hentEtterlysninger(behandling.getId()))
            .filteredOn(e -> e.getStatus() != EtterlysningStatus.AVBRUTT && e.getStatus() != EtterlysningStatus.SKAL_AVBRYTES)
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .as("etterlysningen skal peke på en avklaring i det aktive grunnlaget")
            .allMatch(referanserIAktivtGrunnlag::contains);
    }

    private List<VilkårPeriodeAvklaring> hentSorterteAvklaringer() {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VILKÅR)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
            .toList();
    }

    private static VurderFaktaOmAndreLivsoppholdsytelserDto dtoUtenVarsel(ÅpenPeriode periode, AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak) {
        var avklaring = new AndreLivsoppholdsytelserAvklaringIkkeOppfyltDto(årsak, "begrunnelse", true, null, BEGRUNNELSE_IKKE_VARSEL, AndreLivsoppholdsytelserAvklaringKildeType.NAV, null);
        return new VurderFaktaOmAndreLivsoppholdsytelserDto(List.of(new AndreLivsoppholdsytelserFaktaavklaringPeriodeDto(periode, avklaring)), "begrunnelse");
    }

    private static VurderFaktaOmAndreLivsoppholdsytelserDto dtoMedVarsel(ÅpenPeriode periode, AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak) {
        return dtoMedVarsel(periode, årsak, "begrunnelse");
    }

    private static VurderFaktaOmAndreLivsoppholdsytelserDto dtoMedVarsel(ÅpenPeriode periode, AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak, String begrunnelse) {
        var avklaring = new AndreLivsoppholdsytelserAvklaringIkkeOppfyltDto(årsak, begrunnelse, false, "Fritekst til varsel", null, AndreLivsoppholdsytelserAvklaringKildeType.NAV, null);
        return new VurderFaktaOmAndreLivsoppholdsytelserDto(List.of(new AndreLivsoppholdsytelserFaktaavklaringPeriodeDto(periode, avklaring)), "begrunnelse");
    }
}
