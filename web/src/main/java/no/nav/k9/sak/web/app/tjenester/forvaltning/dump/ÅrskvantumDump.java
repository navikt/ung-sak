package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class ÅrskvantumDump implements DebugDumpBehandling {

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        // TODO Auto-generated method stub
        return null;
    }

}
