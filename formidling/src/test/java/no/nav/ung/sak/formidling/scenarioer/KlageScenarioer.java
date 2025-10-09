package no.nav.ung.sak.formidling.scenarioer;

import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravAdapter;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.test.util.behandling.UngKlageTestScenario;

public class KlageScenarioer {

    /**
     * Klage Avvist
     *
     * @param påklagdBehandling
     * @param begrunnelse
     */


    public static UngKlageTestScenario klageAvvist(Behandling påklagdBehandling, String begrunnelse) {
        var klageUtredning = new KlageUtredningEntitet.Builder()
            .medFormkrav(new KlageFormkravAdapter(
                true,
                false,
                true,
                true,
                true,
                begrunnelse
            ))
            .medpåklagdBehandlingId(påklagdBehandling.getUuid())
            .medOpprinneligBehandlendeEnhet("4806");

        var klageVurdering = new KlageVurderingAdapter(
            KlageVurderingType.AVVIS_KLAGE,
            null,
            null,
            null,
            null,
            null,
            null,
            KlageVurdertAv.VEDTAKSINSTANS
        );

        return new UngKlageTestScenario(klageUtredning, klageVurdering);
    }


}
