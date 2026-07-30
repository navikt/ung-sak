package no.nav.ung.ytelse.aktivitetspenger.testdata;

import jakarta.persistence.EntityManager;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;

/**
 * Hjelpeobjekt for å samle repositories brukt for å lage testdata.
 */
public record AktivitetspengerTestRepositories(
    BehandlingRepositoryProvider repositoryProvider,
    StartdatoRepository startdatoRepository,
    TilkjentYtelseRepository tilkjentYtelseRepository,
    ProsessTriggereRepository prosessTriggereRepository,
    InntektArbeidYtelseTjeneste abakusInMemoryInntektArbeidYtelseTjeneste,
    VedtaksbrevValgRepository vedtaksbrevValgRepository,
    KlageRepository klageRepository,
    FritekstRepository fritekstRepository,
    AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
    BehandlingAnsvarligRepository behandlingAnsvarligRepository) {

    public static AktivitetspengerTestRepositories lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(EntityManager entityManager, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        return lagAlle(entityManager, inntektArbeidYtelseTjeneste, null, null);
    }

    private static AktivitetspengerTestRepositories lagAlle(EntityManager entityManager, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, KlageRepository klageRepository, FritekstRepository fritekstRepository) {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        var prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        var vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
        var aktivitetspengerGrunnlagRepository = new AktivitetspengerGrunnlagRepository(entityManager);
        var behandlingAnsvarligRepository = new BehandlingAnsvarligRepository(entityManager, repositoryProvider.getBehandlingRepository());
        return new AktivitetspengerTestRepositories(repositoryProvider, new StartdatoRepository(entityManager), tilkjentYtelseRepository, prosessTriggereRepository, inntektArbeidYtelseTjeneste, vedtaksbrevValgRepository, klageRepository, fritekstRepository, aktivitetspengerGrunnlagRepository, behandlingAnsvarligRepository);
    }

}
