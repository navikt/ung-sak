package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import javax.enterprise.context.ApplicationScoped;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultHåndtereAutomatiskAvslag implements HåndtereAutomatiskAvslag {

    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode, List<OpptjeningAktivitet> opptjeningAktiveter) {
        // DO NOTHING
    }
}
