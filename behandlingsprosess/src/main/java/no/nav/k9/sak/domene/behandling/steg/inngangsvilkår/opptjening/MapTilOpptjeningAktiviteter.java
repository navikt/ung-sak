package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.ReferanseType;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Aktivitet;

public class MapTilOpptjeningAktiviteter {

    public List<OpptjeningAktivitet> map(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder, OpptjeningAktivitetKlassifisering klassifiseringType) {
        // slå opp fra kodeverk for å sikre instans fra db.
        OpptjeningAktivitetKlassifisering klassifisering = OpptjeningAktivitetKlassifisering.fraKode(klassifiseringType.getKode());
        List<OpptjeningAktivitet> opptjeningAktivitet = new ArrayList<>();
        for (Map.Entry<Aktivitet, LocalDateTimeline<Boolean>> entry : perioder.entrySet()) {
            Aktivitet key = entry.getKey();
            OpptjeningAktivitetType aktType = OpptjeningAktivitetType.fraKode(key.getAktivitetType());
            String aktivitetReferanse = key.getAktivitetReferanse();
            ReferanseType refType = getAktivitetReferanseType(aktivitetReferanse, key);
            for (LocalDateSegment<Boolean> seg : entry.getValue().toSegments()) {
                opptjeningAktivitet.add(new OpptjeningAktivitet(seg.getFom(), seg.getTom(), aktType, klassifisering, aktivitetReferanse, refType));
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
