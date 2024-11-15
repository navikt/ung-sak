package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.abakus;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.iay.modell.Inntektsmelding;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
public class AbakusDump implements DebugDumpBehandling, DebugDumpFagsak {

    private final ObjectWriter iayMapper = JsonObjectMapper.getMapper().writerWithDefaultPrettyPrinter();
    private AbakusTjenesteAdapter tjeneste;

    AbakusDump() {
        // for proxy
    }

    @Inject
    public AbakusDump(AbakusTjenesteAdapter tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            // For kall med systembruker og opprettelse av systemtoken
            var data = ContainerContextRunner.doRun(behandling, () -> tjeneste.finnGrunnlag(behandling.getId()));
            if (data.isEmpty()) {
                return;
            }
            dumpMottaker.newFile(basePath + "/abakus-iaygrunnlag.json");
            iayMapper.writeValue(dumpMottaker.getOutputStream(), data.get());
        } catch (Exception e) {
            dumpMottaker.newFile(basePath + "/abakus-iaygrunnlag-ERROR.txt");
            dumpMottaker.write(e);
        }
    }

    @Override
    public void dump(DumpMottaker dumpMottaker) {
        String relativePath = "abakus-inntektsmeldinger";
        try {
            // For kall med systembruker og opprettelse av systemtoken
            var data = ContainerContextRunner.doRun(dumpMottaker.getFagsak(), () -> tjeneste.hentUnikeInntektsmeldingerForSak(dumpMottaker.getFagsak().getSaksnummer()));
            for (Inntektsmelding im : data) {
                relativePath = "abakus-inntektsmelding-" +
                    im.getArbeidsgiver().getIdentifikator() +
                    "-journalpost_" + im.getJournalpostId().getVerdi() +
                    ".json";
                dumpMottaker.newFile(relativePath);
                iayMapper.writeValue(dumpMottaker.getOutputStream(), im);
            }
        } catch (Exception e) {
            dumpMottaker.newFile(relativePath + "-ERROR.txt");
            dumpMottaker.write(e);
        }
    }
}
