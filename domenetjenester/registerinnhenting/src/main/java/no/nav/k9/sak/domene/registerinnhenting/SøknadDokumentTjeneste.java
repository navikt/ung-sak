package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Optional;

import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;

public interface SøknadDokumentTjeneste {

    public static Optional<SøknadDokumentTjeneste> finnTjeneste(Instance<SøknadDokumentTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(SøknadDokumentTjeneste.class, instances, ytelseType);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId);

    public DiffResult diffResultat(EndringsresultatDiff diff, boolean onlyTrackedFields);

    public Class<?> getGrunnlagsKlasse();
}
