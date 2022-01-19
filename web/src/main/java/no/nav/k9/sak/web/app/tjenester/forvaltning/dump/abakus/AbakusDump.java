package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.abakus;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("FRISINN")
public class AbakusDump implements DebugDumpBehandling, DebugDumpFagsak {

    private final ObjectWriter iayMapper = IayGrunnlagJsonMapper.getMapper().writerWithDefaultPrettyPrinter();
    private AbakusTjenesteAdapter tjeneste;

    AbakusDump() {
        // for proxy
    }

    @Inject
    public AbakusDump(AbakusTjenesteAdapter tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        try {
            var data = tjeneste.finnGrunnlag(behandling.getId());
            if (data.isEmpty()) {
                return List.of();
            }
            var content = iayMapper.writeValueAsString(data.get());
            return List.of(new DumpOutput("abakus-iaygrunnlag.json", content));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return List.of(new DumpOutput("abakus-iaygrunnlag-ERROR.txt", sw.toString()));
        }

    }

    @Override
    public List<DumpOutput> dump(Fagsak fagsak) {
        var resultat = new ArrayList<DumpOutput>();

        String relativePath = "abakus-inntektsmeldinger";
        try {
            var data = tjeneste.hentUnikeInntektsmeldingerForSak(fagsak.getSaksnummer());
            if (data.isEmpty()) {
                return List.of();
            }
            for (var im : data) {
                var content = iayMapper.writeValueAsString(im);
                relativePath = "abakus-inntektsmelding-" + im.getArbeidsgiver().getIdentifikator() + "-journalpost_" + im.getJournalpostId().getVerdi();
                resultat.add(new DumpOutput(relativePath + ".json", content));
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            resultat.add(new DumpOutput(relativePath + "-ERROR.txt", sw.toString()));
        }
        return resultat;
    }

}
