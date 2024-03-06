package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.abakus;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import com.fasterxml.jackson.databind.ObjectWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpFagsak;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
public class AbakusDump implements DebugDumpBehandling, DebugDumpFagsak {

    private final ObjectWriter iayMapper = JsonObjectMapper.getMapper().writerWithDefaultPrettyPrinter();
    private AbakusTjenesteAdapter tjeneste;

    private K9AbakusTjenesteAdapter k9AbakusTjenesteAdapter;

    private boolean k9abakusEnabled;

    AbakusDump() {
        // for proxy
    }

    @Inject
    public AbakusDump(AbakusTjenesteAdapter tjeneste, K9AbakusTjenesteAdapter k9AbakusTjenesteAdapter, @KonfigVerdi(value = "k9.abakus.enabled", defaultVerdi = "false") boolean k9abakusEnabled) {
        this.tjeneste = tjeneste;
        this.k9AbakusTjenesteAdapter = k9AbakusTjenesteAdapter;
        this.k9abakusEnabled = k9abakusEnabled;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        try {
            // For kall med systembruker og opprettelse av systemtoken
            var data = ContainerContextRunner.doRun(behandling, () -> {
                if (k9abakusEnabled) {
                    return k9AbakusTjenesteAdapter.finnGrunnlag(behandling.getId());
                } else {
                    return tjeneste.finnGrunnlag(behandling.getId());
                }
            });
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
            var data = ContainerContextRunner.doRun(dumpMottaker.getFagsak(), () -> {
                if (k9abakusEnabled) {
                    return k9AbakusTjenesteAdapter.hentUnikeInntektsmeldingerForSak(dumpMottaker.getFagsak().getSaksnummer());
                } else {
                    return tjeneste.hentUnikeInntektsmeldingerForSak(dumpMottaker.getFagsak().getSaksnummer());
                }
            });
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
