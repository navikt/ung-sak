package no.nav.ung.sak.test.util.behandling.aktivitetspenger;

import jakarta.persistence.EntityManager;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriodeRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;

/**
 * Hjelpeobjekt for å samle repositories brukt for å lage testdata.
 */
public record AktivitetspengerTestRepositories(
    BehandlingRepositoryProvider repositoryProvider,
    AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository,
    TilkjentYtelseRepository tilkjentYtelseRepository,
    ProsessTriggereRepository prosessTriggereRepository,
    InntektArbeidYtelseTjeneste abakusInMemoryInntektArbeidYtelseTjeneste,
    VedtaksbrevValgRepository vedtaksbrevValgRepository,
    KlageRepository klageRepository,
    FritekstRepository fritekstRepository) {

    public static AktivitetspengerTestRepositories lagAlleAktivitetspengerTestRepositoriesOgAbakusTjeneste(EntityManager entityManager, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        return lagAlle(entityManager, inntektArbeidYtelseTjeneste, null, null);
    }

    private static AktivitetspengerTestRepositories lagAlle(EntityManager entityManager, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, KlageRepository klageRepository, FritekstRepository fritekstRepository) {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var aktivitetspengerSøktPeriodeRepository = new AktivitetspengerSøktPeriodeRepository(entityManager);
        var tilkjentYtelseRepository = new TilkjentYtelseRepository(entityManager);
        var prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        var vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
        return new AktivitetspengerTestRepositories(repositoryProvider, aktivitetspengerSøktPeriodeRepository, tilkjentYtelseRepository, prosessTriggereRepository, inntektArbeidYtelseTjeneste, vedtaksbrevValgRepository, klageRepository, fritekstRepository);
    }

}
