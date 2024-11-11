package no.nav.ung.sak.web.app.tjenester.fagsak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

// View = respons fra applikasjonslag (velges foreløpig fremfor å la applikasjonslag bruke DTO direkte)
public class FagsakSamlingForBruker {

    private Personinfo brukerInfo;
    private List<FagsakRad> fagsakInfoer = new ArrayList<>();

    private FagsakSamlingForBruker() {
    }

    FagsakSamlingForBruker(Personinfo brukerInfo) {
        this.brukerInfo = brukerInfo;
    }

    static FagsakSamlingForBruker emptyView() {
        return new FagsakSamlingForBruker();
    }

    public Personinfo getBrukerInfo() {
        return brukerInfo;
    }

    public List<FagsakRad> getFagsakInfoer() {
        return fagsakInfoer;
    }

    public boolean isEmpty() {
        return brukerInfo == null;
    }

    void leggTil(Fagsak fagsak) {
        fagsakInfoer.add(new FagsakRad(fagsak));
    }

    public static class FagsakRad {
        private final Fagsak fagsak;

        private FagsakRad(Fagsak fagsak) {
            this.fagsak = fagsak;
        }

        public Fagsak getFagsak() {
            return fagsak;
        }
    }
}
