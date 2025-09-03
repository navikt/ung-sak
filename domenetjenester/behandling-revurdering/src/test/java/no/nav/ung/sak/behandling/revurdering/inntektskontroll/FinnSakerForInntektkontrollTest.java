package no.nav.ung.sak.behandling.revurdering.inntektskontroll;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class FinnSakerForInntektkontrollTest {

    public static final LocalDate FØRSTE_SEPTEMBER = LocalDate.of(2025, 9, 1);
    public static final LocalDate MIDT_I_SEPTEMBER = LocalDate.of(2025, 9, 15);
    public static final LocalDate LANGT_FRAM = LocalDate.of(2026, 7, 1);
    public static final LocalDate LANGT_BAK = LocalDate.of(2025, 1, 1);
    public static final LocalDate FØRSTE_AUGUST = LocalDate.of(2025, 8, 1);
    public static final LocalDate SISTE_DAG_I_SEPTEMBER = LocalDate.of(2025, 9, 30);

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    private EntityManager entityManager;

    private FinnSakerForInntektkontroll finnSakerForInntektkontroll;

    private Behandling behandling;

    @BeforeEach
    void setUp() {


        finnSakerForInntektkontroll = new FinnSakerForInntektkontroll(
            behandlingRepository,
            fagsakRepository,
            ungdomsprogramPeriodeRepository,
            prosessTriggereRepository,
            new VilkårTjeneste(behandlingRepository, null, vilkårResultatRepository),
            ungdomsytelseGrunnlagRepository,
            tilkjentYtelseRepository
        );

        behandling = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.UNGDOMSYTELSE)
            .lagre(entityManager);


    }

    @Test
    void skal_ikke_finne_fagsak_uten_programperiode() {


        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();


        assertEquals(0, fagsaker.size());
    }

    @Test
    void skal_ikke_finne_fagsak_for_kontroll_av_første_måned_i_programperiode() {

        opprettProgramperiode(FØRSTE_SEPTEMBER, LANGT_FRAM);


        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();


        assertEquals(0, fagsaker.size());
    }

    @Test
    void skal_finne_fagsak_for_kontroll_av_andre_måned_i_programperiode() {
        opprettProgramperiode(FØRSTE_AUGUST, LANGT_FRAM);

        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();


        assertEquals(1, fagsaker.size());
    }

    @Test
    void skal_ikke_finne_fagsak_for_kontroll_av_siste_måned_i_programperiode() {
        opprettProgramperiode(LANGT_BAK, MIDT_I_SEPTEMBER);


        List<Fagsak> fagsaker = finnFagsakerForInntektskontrollISeptember();


        assertEquals(0, fagsaker.size());
    }



    private void opprettProgramperiode(LocalDate fomDato, LocalDate tomDato) {
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato))));
    }

    private List<Fagsak> finnFagsakerForInntektskontrollISeptember() {
        return finnSakerForInntektkontroll.finnFagsaker(FØRSTE_SEPTEMBER, SISTE_DAG_I_SEPTEMBER);
    }


}
