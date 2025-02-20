package no.nav.ung.sak.test.util;

import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
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
    AbakusInMemoryInntektArbeidYtelseTjeneste abakus) {
}
