package no.nav.foreldrepenger.økonomi.task;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface TaskFeilmeldinger extends DeklarerteFeil {
    TaskFeilmeldinger FACTORY = FeilFactory.create(TaskFeilmeldinger.class);

    @TekniskFeil(feilkode = "FP-765933", feilmelding = "Støtter ikke fagsakYtelseType=%s.", logLevel = LogLevel.WARN)
    Feil støtterIkkeFagsakYtelseType(FagsakYtelseType fagsakYtelseType);
}
