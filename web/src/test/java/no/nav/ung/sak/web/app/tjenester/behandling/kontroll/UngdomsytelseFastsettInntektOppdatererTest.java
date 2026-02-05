package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektBuilder;
import no.nav.ung.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.iay.modell.VersjonType;
import no.nav.ung.sak.kontrakt.kontroll.BrukKontrollertInntektValg;
import no.nav.ung.sak.kontrakt.kontroll.FastsettInntektDto;
import no.nav.ung.sak.kontrakt.kontroll.FastsettInntektPeriodeDto;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.kontroll.RapportertInntektMapper;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseFastsettInntektOppdatererTest {

    public static final LocalDate FOM = LocalDate.now();

    @Inject
    private EntityManager entityManager;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    @Inject
    private ProsessTriggereRepository prosesstriggerRepo;
    @Inject
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    @Inject
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;

    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private FastsettInntektOppdaterer oppdaterer;

    private Behandling behandling;
    private Aksjonspunkt aksjonspunkt;

    @BeforeEach
    void setUp() {
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        final var rapportertInntektMapper = new RapportertInntektMapper(inntektArbeidYtelseTjeneste);
        oppdaterer = new FastsettInntektOppdaterer(
            kontrollerteInntektperioderTjeneste,
            rapportertInntektMapper,
            relevanteKontrollperioderUtleder,
            new HistorikkinnslagRepository(entityManager));

        fagsakRepository = new FagsakRepository(entityManager);

        lagFagsakOgBehandling(FOM);
        lagUngdomsprogramperioder(FOM);
        aksjonspunkt = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
    }

    @Test
    void skal_feile_dersom_valg_brukers_inntekt_uten_rapportert_inntekt_fra_bruker() {
        // Arrange
        final var førsteRapporteringsmånedFom = FOM.plusMonths(1).withDayOfMonth(1);
        final var førsteRapporteringsmånedTom = FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteRapporteringsmånedFom, førsteRapporteringsmånedTom);
        lagreIAYUtenRapportertInntekt();
        lagreRegisterinntekt(periode, 200);
        leggTilTriggerForKontroll(periode);

        final var dto = lagDto(periode, BrukKontrollertInntektValg.BRUK_BRUKERS_INNTEKT);
        final var param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        // Act
        assertThrows(IllegalArgumentException.class, () -> oppdaterer.oppdater(dto, param));
    }

    @Test
    void skal_bekrefte_med_valg_av_brukers_inntekt() {
        // Arrange
        final var førsteRapporteringsmånedFom = FOM.plusMonths(1).withDayOfMonth(1);
        final var førsteRapporteringsmånedTom = FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteRapporteringsmånedFom, førsteRapporteringsmånedTom);
        final var brukersRapporterteInntekt = 100;
        lagreIAYMedRapportertInntekt(periode, brukersRapporterteInntekt);
        lagreRegisterinntekt(periode, 200);
        leggTilTriggerForKontroll(periode);

        final var dto = lagDto(periode, BrukKontrollertInntektValg.BRUK_BRUKERS_INNTEKT);
        final var param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        // Act
        final var resultat = oppdaterer.oppdater(dto, param);

        // Assert
        assertThat(resultat).isNotNull();
        final var kontrollertePerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId());
        assertThat(kontrollertePerioder.get().getPerioder().size()).isEqualTo(1);
        final var kontrollertperiode = kontrollertePerioder.get().getPerioder().get(0);
        assertThat(kontrollertperiode.getKilde()).isEqualTo(KontrollertInntektKilde.BRUKER);
        assertThat(kontrollertperiode.getInntekt().compareTo(BigDecimal.valueOf(brukersRapporterteInntekt))).isEqualTo(0);
    }

    @Test
    void skal_bekrefte_med_valg_av_register_inntekt() {
        // Arrange
        final var førsteRapporteringsmånedFom = FOM.plusMonths(1).withDayOfMonth(1);
        final var førsteRapporteringsmånedTom = FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteRapporteringsmånedFom, førsteRapporteringsmånedTom);
        final var brukersRapporterteInntekt = 100;
        lagreIAYMedRapportertInntekt(periode, brukersRapporterteInntekt);
        final var registerinntekt = 200;
        lagreRegisterinntekt(periode, registerinntekt);
        leggTilTriggerForKontroll(periode);

        final FastsettInntektDto dto = lagDto(periode, BrukKontrollertInntektValg.BRUK_REGISTER_INNTEKT);
        final var param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        // Act
        final var resultat = oppdaterer.oppdater(dto, param);

        // Assert
        assertThat(resultat).isNotNull();
        final var kontrollertePerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId());
        assertThat(kontrollertePerioder.get().getPerioder().size()).isEqualTo(1);
        final var kontrollertperiode = kontrollertePerioder.get().getPerioder().get(0);
        assertThat(kontrollertperiode.getKilde()).isEqualTo(KontrollertInntektKilde.REGISTER);
        assertThat(kontrollertperiode.getInntekt().compareTo(BigDecimal.valueOf(registerinntekt))).isEqualTo(0);
    }

    @Test
    void skal_bekrefte_med_manuelt_fastsatt_inntekt() {
        // Arrange
        final var førsteRapporteringsmånedFom = FOM.plusMonths(1).withDayOfMonth(1);
        final var førsteRapporteringsmånedTom = FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteRapporteringsmånedFom, førsteRapporteringsmånedTom);
        final var brukersRapporterteInntekt = 100;
        lagreIAYMedRapportertInntekt(periode, brukersRapporterteInntekt);
        final var registerinntekt = 200;
        lagreRegisterinntekt(periode, registerinntekt);
        leggTilTriggerForKontroll(periode);

        final var manueltFastsattArbeidsinntekt = 300;
        final var dto = lagDto(periode, manueltFastsattArbeidsinntekt);
        final var param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        // Act
        final var resultat = oppdaterer.oppdater(dto, param);

        // Assert
        assertThat(resultat).isNotNull();
        final var kontrollertePerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId());
        assertThat(kontrollertePerioder.get().getPerioder().size()).isEqualTo(1);
        final var kontrollertperiode = kontrollertePerioder.get().getPerioder().get(0);
        assertThat(kontrollertperiode.getKilde()).isEqualTo(KontrollertInntektKilde.SAKSBEHANDLER);
        assertThat(kontrollertperiode.getInntekt().compareTo(BigDecimal.valueOf(manueltFastsattArbeidsinntekt))).isEqualTo(0);
    }

    @Test
    void skal_bekrefte_med_manuelt_fastsatt_inntekt_når_det_ikke_er_rapportert_eller_register_inntekt() {
        // Arrange
        final var førsteRapporteringsmånedFom = FOM.plusMonths(1).withDayOfMonth(1);
        final var førsteRapporteringsmånedTom = FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteRapporteringsmånedFom, førsteRapporteringsmånedTom);
        lagreIAYUtenRapportertInntekt();
        leggTilTriggerForKontroll(periode);

        final var manueltFastsattArbeidsinntekt = 300;
        final FastsettInntektDto dto = lagDto(periode, manueltFastsattArbeidsinntekt);
        final var param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        // Act
        final var resultat = oppdaterer.oppdater(dto, param);

        // Assert
        assertThat(resultat).isNotNull();
        final var kontrollertePerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId());
        assertThat(kontrollertePerioder.get().getPerioder().size()).isEqualTo(1);
        final var kontrollertperiode = kontrollertePerioder.get().getPerioder().get(0);
        assertThat(kontrollertperiode.getKilde()).isEqualTo(KontrollertInntektKilde.SAKSBEHANDLER);
        assertThat(kontrollertperiode.getInntekt()).isEqualTo(BigDecimal.valueOf(manueltFastsattArbeidsinntekt));
    }

    @Test
    void skal_bekrefte_med_valg_av_register_inntekt_når_det_ikke_er_rapportert_eller_register_inntek() {
        // Arrange
        final var førsteRapporteringsmånedFom = FOM.plusMonths(1).withDayOfMonth(1);
        final var førsteRapporteringsmånedTom = FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        final var periode = DatoIntervallEntitet.fraOgMedTilOgMed(førsteRapporteringsmånedFom, førsteRapporteringsmånedTom);
        lagreIAYUtenRapportertInntekt();
        leggTilTriggerForKontroll(periode);

        final FastsettInntektDto dto = lagDto(periode, BrukKontrollertInntektValg.BRUK_REGISTER_INNTEKT);
        final var param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        // Act
        final var resultat = oppdaterer.oppdater(dto, param);

        // Assert
        assertThat(resultat).isNotNull();
        final var kontrollertePerioder = tilkjentYtelseRepository.hentKontrollertInntektPerioder(behandling.getId());
        assertThat(kontrollertePerioder.get().getPerioder().size()).isEqualTo(1);
        final var kontrollertperiode = kontrollertePerioder.get().getPerioder().get(0);
        assertThat(kontrollertperiode.getKilde()).isEqualTo(KontrollertInntektKilde.REGISTER);
        assertThat(kontrollertperiode.getInntekt().compareTo(BigDecimal.valueOf(0))).isEqualTo(0);
    }

    private static FastsettInntektDto lagDto(DatoIntervallEntitet periode, BrukKontrollertInntektValg brukBrukersInntekt) {
        return new FastsettInntektDto("begrunnelse", List.of(
            new FastsettInntektPeriodeDto(
                new Periode(periode.getFomDato(), periode.getTomDato()), null, brukBrukersInntekt, "en begrunnelse")));
    }

    private static FastsettInntektDto lagDto(DatoIntervallEntitet periode, Integer fastsattInntektDto) {
        return new FastsettInntektDto("begrunnelse", List.of(
            new FastsettInntektPeriodeDto(
                new Periode(periode.getFomDato(), periode.getTomDato()), fastsattInntektDto, BrukKontrollertInntektValg.MANUELT_FASTSATT, "en begrunnelse")));
    }


    private void lagreRegisterinntekt(DatoIntervallEntitet periode, int registerinntekt) {
        final var registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        final var inntekterBuilder = registerBuilder.getInntekterBuilder();
        final var inntektspostBuilder = InntektspostBuilder.ny()
            .medPeriode(periode.getFomDato(), periode.getTomDato())
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(BigDecimal.valueOf(registerinntekt));
        final var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_UNGDOMSYTELSE)
            .leggTilInntektspost(inntektspostBuilder);
        inntekterBuilder.leggTilInntekt(inntektBuilder);
        registerBuilder.leggTilInntekter(inntekterBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), registerBuilder);
    }

    private void leggTilTriggerForKontroll(DatoIntervallEntitet periode) {
        prosesstriggerRepo.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, periode)));
    }

    private void lagUngdomsprogramperioder(LocalDate fom) {
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMed(fom))));
    }


    private void lagreIAYUtenRapportertInntekt() {
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), OppgittOpptjeningBuilder.ny());
    }

    private void lagreIAYMedRapportertInntekt(DatoIntervallEntitet periode, int inntekt) {
        final var ny = OppgittOpptjeningBuilder.ny();
        final var oppgittArbeidBuilder = OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny().medInntekt(BigDecimal.valueOf(inntekt)).medArbeidType(ArbeidType.VANLIG).medPeriode(periode);
        ny.leggTilOppgittArbeidsforhold(oppgittArbeidBuilder);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), ny);
    }


    private Long lagFagsakOgBehandling(LocalDate fom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }


}
