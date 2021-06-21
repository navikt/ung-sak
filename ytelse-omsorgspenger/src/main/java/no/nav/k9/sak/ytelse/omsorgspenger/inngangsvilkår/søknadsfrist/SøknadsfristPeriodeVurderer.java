package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.util.Optional;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartKravDokument;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;

public interface SøknadsfristPeriodeVurderer<T extends SøktPeriodeData> {

    LocalDateTimeline<VurdertSøktPeriode<T>> vurderPeriode(KravDokument søknadsDokument, LocalDateTimeline<SøktPeriode<T>> søktPeriode, Optional<AvklartKravDokument> avklartKravDokument);

    default LocalDateInterval periodeSomVurderes() {
        return new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);
    }
}
