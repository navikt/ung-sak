package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Erstatter det ytelsesspesifikke grunnlaget i en åpen kontrollbehandling med grunnlaget fra en avsluttet ordinær
 * behandling. Grunnlaget avgjør ytelsesperioden, og dermed hvilke måneder kontrollbehandlingen skal kontrollere.
 * <p>
 * Felles grunnlag (personopplysninger, startdatoer, vilkårsresultat og uttalelser) erstattes av
 * {@link SynkroniserÅpneBehandlingerTjeneste}.
 */
public interface YtelsesgrunnlagSynkroniserer {

    static YtelsesgrunnlagSynkroniserer finnTjeneste(Instance<YtelsesgrunnlagSynkroniserer> tjenester, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(tjenester, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Finner ingen YtelsesgrunnlagSynkroniserer for fagsakYtelseType=" + ytelseType));
    }

    void erstattYtelsesgrunnlagFraOrdinærBehandling(Long fraBehandlingId, Long tilKontrollbehandlingId);
}
