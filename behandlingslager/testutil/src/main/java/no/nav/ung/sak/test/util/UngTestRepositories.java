package no.nav.ung.sak.test.util;

import jakarta.persistence.EntityManager;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;

/**
 * Hjelpeobjekt for å samle repositories brukt for å lage testdata.
 */
public record UngTestRepositories(
    BehandlingRepositoryProvider repositoryProvider,
    UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
    UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
    UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository,
    TilkjentYtelseRepository tilkjentYtelseRepository,
    ProsessTriggereRepository prosessTriggereRepository,
    InntektArbeidYtelseTjeneste abakusInMemoryInntektArbeidYtelseTjeneste,
    VedtaksbrevValgRepository vedtaksbrevValgRepository) {

    public static UngTestRepositories lagAlleUngTestRepositoriesOgAbakusTjeneste(EntityManager entityManager, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        var ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        var tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        var prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        var ungdomsytelseStartdatoRepository = new UngdomsytelseStartdatoRepository(entityManager);
        var vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
        return new UngTestRepositories(repositoryProvider, ungdomsytelseGrunnlagRepository, ungdomsprogramPeriodeRepository, ungdomsytelseStartdatoRepository, tilkjentYtelseRepository, prosessTriggereRepository, inntektArbeidYtelseTjeneste, vedtaksbrevValgRepository);

    }
}
