package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.template.dto.ManuellVedtaksbrevDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManueltVedtaksbrevInnholdByggerTest {

    private static final DokumentMalType DOKUMENT_MAL_TYPE = DokumentMalType.INNVILGELSE_DOK;
    private final FakeVedtaksbrevValgRepository fakeRepo = new FakeVedtaksbrevValgRepository();;
    private final ManueltVedtaksbrevInnholdBygger bygger = new ManueltVedtaksbrevInnholdBygger(fakeRepo);;
    private final Behandling behandling = lagBehandling();


    @Test
    void skalGåOkHvisRiktigHtml() {
        String redigertBrevHtml = "<h1>Dette er en overskrift</h1><p>Dette er innholdet i brevet.</p>";
        var valg = opprettValg(redigertBrevHtml);
        fakeRepo.setValg(valg);

        TemplateInnholdResultat bygg = bygger.bygg(behandling, DOKUMENT_MAL_TYPE);

        assertThat(bygg.templateType()).isEqualTo(TemplateType.MANUELT_VEDTAKSBREV);
        assertThat(bygg.automatiskGenerertFooter()).isFalse();
        assertThat(bygg.templateInnholdDto()).isInstanceOf(ManuellVedtaksbrevDto.class);
        var dto = (ManuellVedtaksbrevDto) bygg.templateInnholdDto();
        assertThat(dto.tekstHtml()).isEqualTo(redigertBrevHtml);

    }

    @Test
    void skalKasteFeilHvisHtmlErNull() {
        var valg = opprettValg(null);
        fakeRepo.setValg(valg);

        byggOgAssertFeil("Ingen lagret tekst");
    }

    @Test
    void skalKasteFeilHvisHtmlErBlank() {
        var valg = opprettValg("   ");
        fakeRepo.setValg(valg);

        byggOgAssertFeil("Ingen lagret tekst");
    }

    @Test
    void skalKasteFeilHvisForsteElementIkkeErOverskrift() {
        var valg = opprettValg("<p>Dette er ikke en overskrift</p>");
        fakeRepo.setValg(valg);

        byggOgAssertFeil("må ha overskift som første element");
    }

    @Test
    void skalKasteFeilHvisOverskriftErTom() {
        var valg = opprettValg("<h1>   </h1><p>Innhold</p>");
        fakeRepo.setValg(valg);

        byggOgAssertFeil("har tom overskrift");
    }

    @Test
    void skalKasteFeilHvisHtmlInneholderPreutfyltOverskrift() {
        var valg = opprettValg("<h1>Fyll inn overskrift...</h1><p>Innhold</p>");
        fakeRepo.setValg(valg);

        byggOgAssertFeil("preutfylt overskrift");
    }

    @Test
    void skalKasteFeilHvisHtmlInneholderPreutfyltBrodtekst() {
        var valg = opprettValg("<h1>Gyldig overskrift</h1><p>Fyll inn brødtekst...</p>");
        fakeRepo.setValg(valg);

        byggOgAssertFeil("preutfylt brødtekst");
    }

    @Test
    void skalKasteFeilHvisIngenValgFunnet() {
        byggOgAssertFeil("Ingen lagrede valg for originalDokumentMalType");
    }

    private void byggOgAssertFeil(String Ingen_lagrede_valg_for_originalDokumentMalType) {
        assertThatThrownBy(() -> bygger.bygg(behandling, DOKUMENT_MAL_TYPE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(Ingen_lagrede_valg_for_originalDokumentMalType);
    }

    private static VedtaksbrevValgEntitet opprettValg(String redigertBrevHtml) {
        return new VedtaksbrevValgEntitet(1L, ManueltVedtaksbrevInnholdByggerTest.DOKUMENT_MAL_TYPE, true, false, redigertBrevHtml);
    }


    private static Behandling lagBehandling() {
        Fagsak fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("123"), LocalDate.now(), LocalDate.now());
        return Behandling.nyBehandlingFor(fagsak, BehandlingType.REVURDERING).build();
    }

    private static class FakeVedtaksbrevValgRepository extends VedtaksbrevValgRepository {
        private VedtaksbrevValgEntitet valg;

        public FakeVedtaksbrevValgRepository() {
            super(null);
        }

        public void setValg(VedtaksbrevValgEntitet valg) {
            this.valg = valg;
        }

        @Override
        public Optional<VedtaksbrevValgEntitet> finnVedtakbrevValg(Long behandlingId, DokumentMalType dokumentMalType) {
            return Optional.ofNullable(valg);
        }
    }
}
