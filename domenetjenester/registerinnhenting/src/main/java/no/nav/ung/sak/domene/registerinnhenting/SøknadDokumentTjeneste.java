package no.nav.ung.sak.domene.registerinnhenting;

import java.util.Optional;

import jakarta.enterprise.inject.Instance;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;

public interface SøknadDokumentTjeneste {

    public static Optional<SøknadDokumentTjeneste> finnTjeneste(Instance<SøknadDokumentTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(SøknadDokumentTjeneste.class, instances, ytelseType);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId);

    public DiffResult diffResultat(EndringsresultatDiff diff, boolean onlyTrackedFields);

    public Class<?> getGrunnlagsKlasse();
}
