package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BeregnYtelseStegTest {
    public static final LocalDate FOM = LocalDate.now();
    @Inject
    private EntityManager entityManager;
    private BeregnYtelseSteg beregnYtelseSteg;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        final var månedsvisTidslinjeUtleder = new MånedsvisTidslinjeUtleder(
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository, new UngdomsytelseStartdatoRepository(entityManager)),
            behandlingRepository);
        beregnYtelseSteg = new BeregnYtelseSteg(ungdomsytelseGrunnlagRepository,
            tilkjentYtelseRepository,
            månedsvisTidslinjeUtleder
        );

        fagsakRepository = new FagsakRepository(entityManager);

        lagFagsakOgBehandling(FOM);
        lagUngdomsprogramperioder(FOM);
        lagSatser(FOM);
        lagUttakPerioder(FOM);


        lagreIAYUtenRapportertInntekt();
    }

    private void lagreIAYUtenRapportertInntekt() {
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), OppgittOpptjeningBuilder.ny());
    }

    @Test
    void skal_opprette_tilkjent_ytelse_med_sporing_og_input() {
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();
    }

    @Test
    void skal_få_tilkjent_ytelse_i_periode_uten_inntekt_med_gjenomført_kontroll() {
        final var kontrollertInntektPeriode = KontrollertInntektPeriode.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1).withDayOfMonth(1), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())))
            .medInntekt(BigDecimal.ZERO)
            .medKilde(KontrollertInntektKilde.BRUKER)
            .build();
        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(kontrollertInntektPeriode));
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();

        final var perioder = tilkjentYtelse.get().getPerioder();
        assertThat(perioder.size()).isEqualTo(2);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, FOM.with(TemporalAdjusters.lastDayOfMonth())));
        assertThat(perioder.get(1).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1).withDayOfMonth(1), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())));
    }

    private void lagUngdomsprogramperioder(LocalDate fom) {
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMed(fom))));
    }

    private void lagSatser(LocalDate fom) {
        final var satser = new UngdomsytelseSatser(BigDecimal.valueOf(1000),
            BigDecimal.TEN,
            BigDecimal.ONE,
            UngdomsytelseSatsType.LAV, 0, 0);
        final var ungdomsytelseSatsResultat = new UngdomsytelseSatsResultat(
            new LocalDateTimeline<>(fom, fom.plusWeeks(52), satser),
            "test", "test"
        );
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), ungdomsytelseSatsResultat);
    }

    private void lagUttakPerioder(LocalDate fom) {
        final var uttakperioder = new UngdomsytelseUttakPerioder(List.of(new UngdomsytelseUttakPeriode(
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusWeeks(52))
        )));
        uttakperioder.setRegelInput("test");
        uttakperioder.setRegelSporing("test");
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), uttakperioder);
    }


    private Long lagFagsakOgBehandling(LocalDate fom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }



}
