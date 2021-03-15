package no.nav.k9.sak.domene.risikoklassifisering.konsument;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.domene.risikoklassifisering.json.KontrollSerialiseringUtil;
import no.nav.k9.sak.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.KontrollresultatWrapper;
import no.nav.k9.sak.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.vedtak.kontroll.v1.KontrollResultatV1;

@ProsessTask(LesKontrollresultatTask.TASKTYPE)
public class LesKontrollresultatTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "risiko.klassifisering.resultat";
    private static final Logger LOGGER = LoggerFactory.getLogger(LesKontrollresultatTask.class);
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private KontrollresultatMapper kontrollresultatMapper;

    public LesKontrollresultatTask() {
    }

    @Inject
    public LesKontrollresultatTask(RisikovurderingTjeneste risikovurderingTjeneste,
                                   KontrollresultatMapper kontrollresultatMapper) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.kontrollresultatMapper = kontrollresultatMapper;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        String payload = prosessTaskData.getPayloadAsString();
        try {
            KontrollResultatV1 kontraktResultat = KontrollSerialiseringUtil.deserialiser(payload, KontrollResultatV1.class);
            evaluerKontrollresultat(kontraktResultat);
        } catch (Exception e) {
            LOGGER.warn("Klarte ikke behandle risikoklassifiseringresultat", e);
        }
    }

    private void evaluerKontrollresultat(KontrollResultatV1 kontraktResultat) {
        KontrollresultatWrapper resultatWrapper = kontrollresultatMapper.fraKontrakt(kontraktResultat);
        risikovurderingTjeneste.lagreKontrollresultat(resultatWrapper);
    }


}
