package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

import java.util.List;

public interface HåndtereAutomatiskAvslag {

    void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode, List<OpptjeningAktivitet> opptjeningsperiode);
}
