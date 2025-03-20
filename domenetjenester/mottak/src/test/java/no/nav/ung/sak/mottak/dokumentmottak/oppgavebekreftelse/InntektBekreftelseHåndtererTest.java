package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.oppgave.OppgaveBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.InntektBekreftelse;
import no.nav.k9.oppgave.bekreftelse.ung.inntekt.OppgittInntektForPeriode;
import no.nav.k9.søknad.felles.Versjon;
import no.nav.k9.søknad.felles.personopplysninger.Søker;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.felles.type.SøknadId;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningEntitet;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.JournalpostId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InntektBekreftelseHåndtererTest {

    @Inject
    private EntityManager em;
    private EtterlysningRepository etterlysningRepository;
    private AbakusInMemoryInntektArbeidYtelseTjeneste abakusInMemoryInntektArbeidYtelseTjeneste;


    @BeforeEach
    void setup() {
        etterlysningRepository = new EtterlysningRepository(em);
        abakusInMemoryInntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    }

    @Test
    void skalOppdatereEtterlysningOppdatereIayGrunnlagLagreUttalelseOgSetteBehandlingAvVent() {


        // Arrange
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING);

        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        Behandling behandling = scenarioBuilder.lagre(em);

        var opptjening = OppgittOpptjeningBuilder.ny();
        var periode = DatoIntervallEntitet.fra(LocalDate.now(), LocalDate.now());
//        opptjening.leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
//            .medInntekt(BigDecimal.valueOf(10000))
//            .medPeriode(periode)
//        );

//        var oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
//        var aktørInntektBuilder = oppdatere.getAktørInntektBuilder(behandling.getAktørId());
//        InntektBuilder oppdatere1 = InntektBuilder.oppdatere(Optional.empty());
//        oppdatere1.leggTilInntektspost(InntektspostBuilder.ny()
//            .medInntektspostType(InntektspostType.LØNN.getKode())
//            .medBeløp(BigDecimal.valueOf(5000))
//        );
//
//        InntektArbeidYtelseGrunnlagBuilder nytt = InntektArbeidYtelseGrunnlagBuilder.nytt();
//        aktørInntektBuilder.leggTilInntekt(oppdatere1);
//        nytt.medRegister(oppdatere);

        abakusInMemoryInntektArbeidYtelseTjeneste.lagreOppgittOpptjening(
            behandling.getId(),
            opptjening
        );

        var oppgaveId = UUID.randomUUID();
        var etterlysning = etterlysningRepository.lagre(EtterlysningEntitet.forInntektKontrollUttalelse(
            behandling.getId(),
            abakusInMemoryInntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId()).getEksternReferanse(),
            oppgaveId,
            periode));

        etterlysning.vent(LocalDateTime.now().plusDays(1));
        etterlysningRepository.lagre(etterlysning);
        em.flush();


        var bekreftelse = new OppgaveBekreftelseInnhold(
            new JournalpostId(123L),
            behandling,
            new OppgaveBekreftelse(
                new SøknadId("456"),
                Versjon.of("1"),
                ZonedDateTime.now(),
                new Søker(NorskIdentitetsnummer.of("12345678910")),
                new InntektBekreftelse(
                    oppgaveId,
                    Set.of(new OppgittInntektForPeriode(
                        new Periode(periode.getFomDato(), periode.getTomDato()),
                        BigDecimal.valueOf(10000),
                        BigDecimal.ZERO)),
                    true,
                    "uttalelse")
            )
        );

        // Act
        new InntektBekreftelseHåndterer(etterlysningRepository).håndter(bekreftelse);

        // Assert
        var oppdatertEtterlysning = etterlysningRepository.hentEtterlysning(etterlysning.getId());
        //etterlysning er oppdatert
        assertThat(oppdatertEtterlysning.getStatus()).isEqualTo(EtterlysningStatus.MOTTATT_SVAR);
        //behandling er ikke lenger på vent
        assertThat(behandling.getAksjonspunkter()).isEmpty();
        //abakus er oppdatert
        InntektArbeidYtelseGrunnlag nyttGrunnlag = abakusInMemoryInntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        BigDecimal inntekt = nyttGrunnlag.getOppgittOpptjening().get().getOppgittArbeidsforhold().getFirst().getInntekt();
        assertThat(inntekt).isEqualTo(BigDecimal.valueOf(10000));
    }
}
