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
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.*;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.kontroll.BrukKontrollertInntektValg;
import no.nav.ung.sak.kontrakt.kontroll.FastsettInntektDto;
import no.nav.ung.sak.kontrakt.kontroll.FastsettInntektPeriodeDto;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
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
class FastsettInntektOppdatererTest {


    public static final LocalDate FOM = LocalDate.now();
    @Inject
    private EntityManager entityManager;
    private FastsettInntektOppdaterer oppdaterer;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private Behandling behandling;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private ProsessTriggereRepository prosesstriggerRepo;

    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private Aksjonspunkt aksjonspunkt;

    @BeforeEach
    void setUp() {
        tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        prosesstriggerRepo = new ProsessTriggereRepository(entityManager);
        final var månedsvisTidslinjeUtleder = new MånedsvisTidslinjeUtleder(
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository),
            behandlingRepository);
        prosessTriggerPeriodeUtleder = new ProsessTriggerPeriodeUtleder(prosesstriggerRepo,
            new UngdomsytelseSøknadsperiodeTjeneste(new UngdomsytelseStartdatoRepository(entityManager), new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository), behandlingRepository));
        kontrollerteInntektperioderTjeneste = new KontrollerteInntektperioderTjeneste(tilkjentYtelseRepository, månedsvisTidslinjeUtleder);
        final var rapportertInntektMapper = new RapportertInntektMapper(inntektArbeidYtelseTjeneste, månedsvisTidslinjeUtleder);
        oppdaterer = new FastsettInntektOppdaterer(
            kontrollerteInntektperioderTjeneste,
            rapportertInntektMapper,
            prosessTriggerPeriodeUtleder);

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

        final var dto = lagDto(periode, BrukKontrollertInntektValg.BRUK_REGISTER_INNTEKT);
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
        final var aktørInntektBuilder = registerBuilder.getAktørInntektBuilder(behandling.getAktørId());
        final var inntektspostBuilder = InntektspostBuilder.ny()
            .medPeriode(periode.getFomDato(), periode.getTomDato())
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(BigDecimal.valueOf(registerinntekt));
        final var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_SAMMENLIGNING)
            .leggTilInntektspost(inntektspostBuilder);
        aktørInntektBuilder.leggTilInntekt(inntektBuilder);
        registerBuilder.leggTilAktørInntekt(aktørInntektBuilder);
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
