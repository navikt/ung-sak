package no.nav.k9.sak.domene.medlem.impl;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.medlem.DummyVilkårsVurderingTjeneste;
import no.nav.k9.sak.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class UtledVurderingsdatoerForMedlemskapTjenesteImplTest {

    private static final LocalDate DAGENS_DATO = LocalDate.now();

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private BehandlingRepository behandlingRepository = provider.getBehandlingRepository();
    private MedlemskapRepository medlemskapRepository = provider.getMedlemskapRepository();
    private PersonopplysningRepository personopplysningRepository = provider.getPersonopplysningRepository();
    private FagsakRepository fagsakRepository = provider.getFagsakRepository();

    @Inject
    private UtledVurderingsdatoerForMedlemskapTjeneste tjeneste;

    @Inject
    @FagsakYtelseTypeRef
    private DummyVilkårsVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Test
    public void skal_ikke_utlede_dato_når_overlappende_perioder_uten_endring_i_medl() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FULL)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medPeriode(LocalDate.of(2014, 2, 17), TIDENES_ENDE)
            .medKildeType(MedlemskapKildeType.AVGSYS)
            .medBeslutningsdato(LocalDate.of(2014, 3, 5))
            .medMedlId(1L)
            .build();
        MedlemskapPerioderEntitet periode2 = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FULL)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medPeriode(LocalDate.of(2017, 1, 1), TIDENES_ENDE)
            .medKildeType(MedlemskapKildeType.AVGSYS)
            .medBeslutningsdato(LocalDate.of(2018, 8, 31))
            .medMedlId(1L)
            .build();

        scenario.leggTilMedlemskapPeriode(periode);
        scenario.leggTilMedlemskapPeriode(periode2);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);
        Long revurderingId = revudering.getId();

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(revurderingId);

        // Assert
        assertThat(vurderingsdatoer).containsExactly(DAGENS_DATO);
    }

    @Test
    public void skal_utlede_dato_når_overlappende_perioder_med_endring_i_periode_med_senest_beslutningsdato() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FULL)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medPeriode(LocalDate.of(2014, 2, 17), TIDENES_ENDE)
            .medKildeType(MedlemskapKildeType.AVGSYS)
            .medBeslutningsdato(LocalDate.of(2014, 3, 5))
            .medMedlId(1L)
            .build();
        MedlemskapPerioderEntitet periode2 = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FULL)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medPeriode(LocalDate.of(2017, 1, 1), TIDENES_ENDE)
            .medKildeType(MedlemskapKildeType.AVGSYS)
            .medBeslutningsdato(LocalDate.of(2018, 8, 31))
            .medMedlId(1L)
            .build();

        scenario.leggTilMedlemskapPeriode(periode);
        scenario.leggTilMedlemskapPeriode(periode2);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);
        Long revurderingId = revudering.getId();

        LocalDate endringsdato = DAGENS_DATO.plusMonths(1);
        MedlemskapPerioderEntitet endretPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FULL)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medPeriode(endringsdato, TIDENES_ENDE)
            .medKildeType(MedlemskapKildeType.AVGSYS)
            .medBeslutningsdato(LocalDate.of(2018, 8, 31))
            .medMedlId(1L)
            .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(revurderingId, List.of(periode, endretPeriode));

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(revurderingId);

        // Assert
        assertThat(vurderingsdatoer).containsExactly(DAGENS_DATO, endringsdato);
    }


    @Test
    public void skal_utled_vurderingsdato_ved_endring_i_medlemskapsperioder() {
        // Arrange
        LocalDate datoMedEndring = DAGENS_DATO.plusDays(10);
        LocalDate ettÅrSiden = DAGENS_DATO.minusYears(1);
        LocalDate iDag = DAGENS_DATO;
        var scenario = TestScenarioBuilder.builderMedSøknad();
        MedlemskapPerioderEntitet periode = opprettPeriode(ettÅrSiden, iDag, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);
        Long revurderingId = revudering.getId();

        oppdaterMedlem(datoMedEndring, periode, revurderingId);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(revurderingId);

        // Assert
        assertThat(vurderingsdatoer).containsExactly(DAGENS_DATO, datoMedEndring);
    }

    @Test
    public void skal_utled_vurderingsdato_ved_endring_personopplysninger_statsborgerskap() {
        // Arrange
        LocalDate iDag = DAGENS_DATO;
        var scenario = TestScenarioBuilder.builderMedSøknad();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag, iDag.plusYears(1));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.plusYears(1), iDag.plusYears(2));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.plusYears(2), iDag.plusYears(3));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(), PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.StatsborgerskapBuilder norgeFørsteÅr = personInformasjonBuilder.getStatsborgerskapBuilder(søkerAktørId, førsteÅr, Landkoder.NOR, Region.NORDEN);
        PersonInformasjonBuilder.StatsborgerskapBuilder spaniaAndreÅr = personInformasjonBuilder.getStatsborgerskapBuilder(søkerAktørId, andreÅr, Landkoder.ESP, Region.EOS);
        PersonInformasjonBuilder.StatsborgerskapBuilder norgeTredjeÅr = personInformasjonBuilder.getStatsborgerskapBuilder(søkerAktørId, tredjeÅr, Landkoder.NOR, Region.NORDEN);
        personInformasjonBuilder.leggTil(norgeFørsteÅr);
        personInformasjonBuilder.leggTil(spaniaAndreÅr);
        personInformasjonBuilder.leggTil(norgeTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(behandlingId);

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(DAGENS_DATO, andreÅr.getFomDato(), tredjeÅr.getFomDato());
    }

    @Test
    public void skal_utled_vurderingsdato_ved_endring_personopplysninger_personstatus_skal_ikke_se_på_død() {
        // Arrange
        LocalDate iDag = DAGENS_DATO;
        var scenario = TestScenarioBuilder.builderMedSøknad();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag, iDag.plusYears(1));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.plusYears(1), iDag.plusYears(2));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.plusYears(2), iDag.plusYears(3));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(), PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.PersonstatusBuilder førsteÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, førsteÅr).medPersonstatus(PersonstatusType.BOSA);
        PersonInformasjonBuilder.PersonstatusBuilder andreÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, andreÅr).medPersonstatus(PersonstatusType.UTVA);
        PersonInformasjonBuilder.PersonstatusBuilder tredjeÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, tredjeÅr).medPersonstatus(PersonstatusType.DØD);
        personInformasjonBuilder.leggTil(førsteÅrBosa);
        personInformasjonBuilder.leggTil(andreÅrBosa);
        personInformasjonBuilder.leggTil(tredjeÅrBosa);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(behandlingId);

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(DAGENS_DATO, andreÅr.getFomDato());
    }

    @Test
    public void skal_utled_vurderingsdato_ved_endring_personopplysninger_adressetype() {
        // Arrange
        LocalDate iDag = DAGENS_DATO;
        var scenario = TestScenarioBuilder.builderMedSøknad();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag, iDag.plusYears(1));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.plusYears(1), iDag.plusYears(2));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.plusYears(2), iDag.plusYears(3));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(), PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.AdresseBuilder bostedFørsteÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, førsteÅr, AdresseType.BOSTEDSADRESSE);
        PersonInformasjonBuilder.AdresseBuilder utlandAndreÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, andreÅr, AdresseType.POSTADRESSE_UTLAND);
        PersonInformasjonBuilder.AdresseBuilder bostedTredjeÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, tredjeÅr, AdresseType.BOSTEDSADRESSE);
        personInformasjonBuilder.leggTil(bostedFørsteÅr);
        personInformasjonBuilder.leggTil(utlandAndreÅr);
        personInformasjonBuilder.leggTil(bostedTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(behandlingId);

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(DAGENS_DATO, andreÅr.getFomDato(), tredjeÅr.getFomDato());
    }

    @Test
    public void skal_ikke_utlede_vurderingsdato_som_ligger_før_skjæringstidspunkt() {
        // Arrange
        LocalDate iDag = DAGENS_DATO;
        var scenario = TestScenarioBuilder.builderMedSøknad();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusYears(3), iDag.minusYears(2));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusYears(2), iDag.minusYears(1));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(iDag, iDag.plusYears(1));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(), PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.AdresseBuilder bostedFørsteÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, førsteÅr, AdresseType.BOSTEDSADRESSE);
        PersonInformasjonBuilder.AdresseBuilder utlandAndreÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, andreÅr, AdresseType.POSTADRESSE_UTLAND);
        PersonInformasjonBuilder.AdresseBuilder bostedTredjeÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, tredjeÅr, AdresseType.BOSTEDSADRESSE);
        personInformasjonBuilder.leggTil(bostedFørsteÅr);
        personInformasjonBuilder.leggTil(utlandAndreÅr);
        personInformasjonBuilder.leggTil(bostedTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(behandlingId);

        assertThat(vurderingsdatoer).containsExactly(DAGENS_DATO);
    }

    private void oppdaterMedlem(LocalDate datoMedEndring, MedlemskapPerioderEntitet periode, Long behandlingId) {
        MedlemskapPerioderEntitet nyPeriode = new MedlemskapPerioderBuilder()
            .medPeriode(datoMedEndring, null)
            .medDekningType(MedlemskapDekningType.FULL)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medKildeType(MedlemskapKildeType.MEDL)
            .medMedlId(2L)
            .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(periode, nyPeriode));
    }

    private Behandling opprettRevudering(Behandling behandling) {
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA);

        Behandling revudering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(revurderingÅrsak).build();

        Long behandlingId = behandling.getId();
        // TODO(FC): Her burde kanskje behandlingId vært låst inntil revurdering er opprettet?
        behandlingRepository.lagre(revudering, behandlingRepository.taSkriveLås(revudering.getId()));
        Long revurderingId = revudering.getId();
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(behandlingId, revurderingId);

        return revudering;
    }

    private void avslutterBehandlingOgFagsak(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.AVSLUTTET);
    }

    private MedlemskapPerioderEntitet opprettPeriode(LocalDate fom, LocalDate tom, MedlemskapDekningType dekningType) {
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
            .medDekningType(dekningType)
            .medMedlemskapType(MedlemskapType.FORELOPIG)
            .medPeriode(fom, tom)
            .medKildeType(MedlemskapKildeType.MEDL)
            .medMedlId(1L)
            .build();
        return periode;
    }

}
