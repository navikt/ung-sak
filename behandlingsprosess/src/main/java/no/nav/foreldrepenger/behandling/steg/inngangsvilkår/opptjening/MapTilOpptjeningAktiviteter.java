package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.ReferanseType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class MapTilOpptjeningAktiviteter {

    public MapTilOpptjeningAktiviteter() {
    }

    public List<OpptjeningAktivitet> map(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder,
            OpptjeningAktivitetKlassifisering klassifiseringType) {
        // slå opp fra kodeverk for å sikre instans fra db.
        OpptjeningAktivitetKlassifisering klassifisering = OpptjeningAktivitetKlassifisering.fraKode(klassifiseringType.getKode());
        List<OpptjeningAktivitet> opptjeningAktivitet = new ArrayList<>();
        for (Map.Entry<Aktivitet, LocalDateTimeline<Boolean>> entry : perioder.entrySet()) {
            for (LocalDateSegment<Boolean> seg : entry.getValue().toSegments()) {
                Aktivitet key = entry.getKey();
                OpptjeningAktivitetType aktType = OpptjeningAktivitetType.fraKode(key.getAktivitetType());
                String aktivitetReferanse = key.getAktivitetReferanse();
                ReferanseType refType = getAktivitetReferanseType(aktivitetReferanse, key);

                OpptjeningAktivitet oppAkt = new OpptjeningAktivitet(seg.getFom(), seg.getTom(), aktType, klassifisering,
                        aktivitetReferanse, refType);
                opptjeningAktivitet.add(oppAkt);
            }
        }
        return opptjeningAktivitet;
    }

    private ReferanseType getAktivitetReferanseType(String aktivitetReferanse, Aktivitet key) {
        if (aktivitetReferanse != null) {
            if (key.getReferanseType() == Aktivitet.ReferanseType.ORGNR) {
                return ReferanseType.ORG_NR;
            } else if (key.getReferanseType() == Aktivitet.ReferanseType.AKTØRID) {
                return ReferanseType.AKTØR_ID;
            } else {
                throw new IllegalArgumentException(
                        "Utvikler-feil: Mangler aktivitetReferanseType for aktivitetReferanse[" //$NON-NLS-1$
                                + key.getReferanseType()
                                + "]: " //$NON-NLS-1$
                                + aktivitetReferanse);
            }

        }

        return null;
    }
}
