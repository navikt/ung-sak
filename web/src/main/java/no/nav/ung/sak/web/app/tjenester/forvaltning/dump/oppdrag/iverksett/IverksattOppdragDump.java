package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.oppdrag.iverksett;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;
import no.nav.ung.sak.økonomi.simulering.klient.K9OppdragRestKlient;
import no.nav.ung.sak.økonomi.simulering.klient.dto.OppdragXmlDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class IverksattOppdragDump implements DebugDumpFagsak {

    private static final Logger LOG = LoggerFactory.getLogger(IverksattOppdragDump.class);

    private K9OppdragRestKlient restKlient;

    IverksattOppdragDump() {
        // for proxy
    }

    @Inject
    public IverksattOppdragDump(K9OppdragRestKlient restKlient) {
        this.restKlient = restKlient;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        List<OppdragXmlDto> resultat = restKlient.alleOppdragXmler(dumpMottaker.getFagsak().getSaksnummer());
        int meldingTeller = 1;
        for (OppdragXmlDto xml : resultat) {
            dumpMottaker.newFile("k9oppdrag/iverksatt/" + meldingTeller + "/sendt.xml");
            dumpMottaker.write(new String(Base64.getDecoder().decode(xml.getBase64oppdragXml()), StandardCharsets.UTF_8));
            if (xml.getOppdragKvitteringDto() != null) {
                try {
                    dumpMottaker.newFile("k9oppdrag/iverksatt/" + meldingTeller + "/kvittering.json");
                    dumpMottaker.write(JsonObjectMapper.getJson(xml.getOppdragKvitteringDto()));
                } catch (IOException e) {
                    dumpMottaker.write(e.getMessage());
                    LOG.warn("Feil ved dump av oppdrag-xml", e);
                }
                dumpMottaker.newFile("k9oppdrag/iverksatt/" + meldingTeller + "/meta.txt");
                dumpMottaker.write("behandlingUuid=" + xml.getBehandlingUuid() + "\n");
                dumpMottaker.write("opprettetTidspunkt=" + xml.getOpprettetTidspunkt() + "\n");
            }
            meldingTeller++;
        }
    }

}
