package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.KodeverdiSomObjekt;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.BrevXhtmlTilSeksjonKonverter;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrev;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevReglerUng;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValg;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgResponse;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevEditorResponse;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class VedtaksbrevTjeneste {

    private BehandlingRepository behandlingRepository;
    private VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;
    private VedtaksbrevReglerUng vedtaksbrevRegler;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevTjeneste.class);

    public VedtaksbrevTjeneste() {
    }

    @Inject
    public VedtaksbrevTjeneste(
        VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste,
        @Any VedtaksbrevReglerUng vedtaksbrevRegler,
        VedtaksbrevValgRepository vedtaksbrevValgRepository,
        BehandlingRepository behandlingRepository) {
        this.vedtaksbrevGenerererTjeneste = vedtaksbrevGenerererTjeneste;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public VedtaksbrevValgResponse vedtaksbrevValg(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var erAvsluttet = behandling.erAvsluttet();


        BehandlingVedtaksbrevResultat totalResultat = vedtaksbrevRegler.kjør(behandlingId);
        LOG.info("Regel resultater: {}", totalResultat.safePrint());

        if (!totalResultat.harBrev()) {
            return mapIngenBrevResponse(totalResultat);
        }

        var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId);
        var deaktiverteValg = vedtaksbrevValgRepository.finnNyesteDeaktiverteVedtakbrevValg(behandlingId);
        var vedtaksbrevValg = mapVedtaksbrevValg(totalResultat, valg, deaktiverteValg, erAvsluttet);

        return new VedtaksbrevValgResponse(true, vedtaksbrevValg);
    }

    private static List<VedtaksbrevValg> mapVedtaksbrevValg(BehandlingVedtaksbrevResultat totalResultat, List<VedtaksbrevValgEntitet> valg, List<VedtaksbrevValgEntitet> deaktiverteValg, boolean erAvsluttet) {
        return totalResultat.vedtaksbrevResultater().stream()
            .map(vedtaksbrev -> {
                var malValg = valg.stream().filter(v -> v.getDokumentMalType() == vedtaksbrev.dokumentMalType()).findFirst();
                var deaktivertValg = deaktiverteValg.stream().filter(v -> v.getDokumentMalType() == vedtaksbrev.dokumentMalType()).findFirst();
                return mapVedtaksbrevValg(vedtaksbrev, malValg, deaktivertValg, erAvsluttet);
            })
            .toList();
    }

    private static VedtaksbrevValg mapVedtaksbrevValg(Vedtaksbrev resultat, Optional<VedtaksbrevValgEntitet> valg, Optional<VedtaksbrevValgEntitet> deaktivertValg, boolean erAvsluttet) {
        var egenskaper = resultat.vedtaksbrevEgenskaper();
        String redigertBrevHtml = valg.map(VedtaksbrevValgEntitet::getRedigertBrevHtml).orElse(null);

        var tidligereRedigertTekst = redigertBrevHtml == null ? deaktivertValg
            .map(VedtaksbrevValgEntitet::getRedigertBrevHtml)
            .orElse(null) : null;

        return new VedtaksbrevValg(
            new KodeverdiSomObjekt<>(resultat.dokumentMalType()), egenskaper.kanHindre(),
            valg.map(VedtaksbrevValgEntitet::isHindret).orElse(false),
            !erAvsluttet && egenskaper.kanOverstyreHindre(),
            egenskaper.kanRedigere(),
            valg.map(VedtaksbrevValgEntitet::isRedigert).orElse(false),
            !erAvsluttet && egenskaper.kanOverstyreRediger(),
            resultat.forklaring(),
            redigertBrevHtml,
            tidligereRedigertTekst);
    }

    private static VedtaksbrevValgResponse mapIngenBrevResponse(BehandlingVedtaksbrevResultat totalResultat) {
        return new VedtaksbrevValgResponse(
            false,
            totalResultat.ingenBrevResultater().stream().map(it ->
                new VedtaksbrevValg(
                    null,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    it.forklaring(),
                    null,
                    null
                )).toList()
        );
    }

    public VedtaksbrevValgEntitet lagreVedtaksbrev(VedtaksbrevValgRequest dto) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingId());
        if (behandling.erAvsluttet()) {
            throw new IllegalArgumentException("Kan ikke endre vedtaksbrev på avsluttet behandling");
        }

        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(dto.behandlingId());
        if (!totalresultater.harBrev()) {
            throw new IllegalArgumentException("Ingen vedtaksbrev resultater for behandling");
        }

        Vedtaksbrev vedtaksbrev = totalresultater.finnVedtaksbrev(dto.dokumentMalType())
            .orElseThrow(() -> new IllegalArgumentException("Ingen vedtaksbrev med mal " + dto.dokumentMalType() + " for behandling " + dto.behandlingId()));

        var vedtaksbrevValgEntitet = vedtaksbrevValgRepository.finnVedtakbrevValg(dto.behandlingId(), vedtaksbrev.dokumentMalType())
            .orElse(VedtaksbrevValgEntitet.ny(dto.behandlingId(), vedtaksbrev.dokumentMalType()));

        var vedtaksbrevEgenskaper = vedtaksbrev.vedtaksbrevEgenskaper();

        boolean redigert = Boolean.TRUE.equals(dto.redigert());
        if (!vedtaksbrevEgenskaper.kanRedigere() && redigert) {
            throw new IllegalArgumentException("Brevet kan ikke redigeres.");
        }

        boolean hindret = Boolean.TRUE.equals(dto.hindret());
        if (!vedtaksbrevEgenskaper.kanHindre() && hindret) {
            throw new IllegalArgumentException("Brevet kan ikke hindres. ");
        }

        vedtaksbrevValgEntitet.setHindret(hindret);
        vedtaksbrevValgEntitet.setRedigert(redigert);
        vedtaksbrevValgEntitet.rensOgSettRedigertHtml(dto.redigertHtml());

        LOG.info("Lagrer vedtaksbrevvalg for dokumentMalType={} med verdier redigert={} hindret={} redigertHtml={}",
            dto.dokumentMalType(), vedtaksbrevValgEntitet.isRedigert(), vedtaksbrevValgEntitet.isHindret(),
            vedtaksbrevValgEntitet.getRedigertBrevHtml() != null);

        return vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);

    }

    public GenerertBrev forhåndsvis(VedtaksbrevForhåndsvisInput dto) {
        Long behandlingId = dto.behandlingId();
        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandlingId);
        validerHarBrev(totalresultater);

        var vedtaksbrev = totalresultater.finnVedtaksbrev(dto.dokumentMalType())
            .orElseThrow(() -> new IllegalArgumentException("Støtter ikke mal " + dto.dokumentMalType()));

        if (dto.redigertVersjon() == null) {
            return genererFraValg(behandlingId, vedtaksbrev, dto.htmlVersjon(), totalresultater.detaljertResultatTimeline());
        }

        if (dto.redigertVersjon()) {
            var valg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId, vedtaksbrev.dokumentMalType())
                .orElseThrow(() -> new IllegalStateException("Ingen lagret valg for dokumentMaltype " + vedtaksbrev.dokumentMalType()));
            return vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(behandlingId, valg.getRedigertBrevHtml(), dto.htmlVersjon());
        }

        return vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(
            new VedtaksbrevGenerererInput(
                behandlingId,
                vedtaksbrev,
                totalresultater.detaljertResultatTimeline(),
                dto.htmlVersjon()
            ));

    }

    //Brukes foreløpig bare i test - gir ikke mening å generere alle i ett kall.
    public List<GenerertBrev> genererAlleForBehandling(Long behandlingId, Boolean kunHtml) {
        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(behandlingId);
        validerHarBrev(totalresultater);

        List<GenerertBrev> genererteBrev = totalresultater.vedtaksbrevResultater().stream()
            .map(it -> genererFraValg(
                behandlingId, it, kunHtml, totalresultater.detaljertResultatTimeline()
            ))
            .toList();

        if (genererteBrev.isEmpty()) {
            throw new IllegalArgumentException("Ingen vedtaksbrev generert for behandling.");
        }

        return genererteBrev;
    }

    private static void validerHarBrev(BehandlingVedtaksbrevResultat totalresultater) {
        if (!totalresultater.harBrev()) {
            throw new IllegalArgumentException("Ingen vedtaksbrev resultater for behandling. Årsak: " + totalresultater.ingenBrevResultater().stream()
                .map(IngenBrev::forklaring)
                .collect(Collectors.joining(", ", "[", "]")));
        }
    }

    private GenerertBrev genererFraValg(Long behandlingId, Vedtaksbrev relevantVedtaksbrev, boolean kunHtml, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        var dokumentMalType = relevantVedtaksbrev.dokumentMalType();
        var relevantValg = vedtaksbrevValgRepository
            .finnVedtakbrevValg(behandlingId, dokumentMalType).stream()
            .findFirst();

        if (relevantValg.isPresent()) {
            var valg = relevantValg.get();
            if (valg.isHindret()) {
                throw new IllegalArgumentException("Kan ikke forhåndsvise hindret brev");
            }
            if (valg.isRedigert()) {
                return vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(behandlingId, valg.getRedigertBrevHtml(), kunHtml);
            }
        }

        return vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(
            new VedtaksbrevGenerererInput(
                behandlingId,
                relevantVedtaksbrev,
                detaljertResultatTidslinje,
                kunHtml
            ));
    }

    public void ryddVedTilbakeHopp(Long behandlingId) {
        LOG.info("Fjerner vedtaksbrevvalg");
        vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId)
            .forEach(valg -> {
                valg.deaktiver();
                vedtaksbrevValgRepository.lagre(valg);
            });


    }

    public VedtaksbrevEditorResponse editor(Long behandlingId, DokumentMalType dokumentMalType, boolean redigertVersjon) {
        GenerertBrev forhåndsvis = forhåndsvis(new VedtaksbrevForhåndsvisInput(
            behandlingId,
            dokumentMalType,
            redigertVersjon,
            true
        ));

        List<VedtaksbrevSeksjon> seksjoner = BrevXhtmlTilSeksjonKonverter.konverter(forhåndsvis.dokument().html());

        return new VedtaksbrevEditorResponse(seksjoner);
    }
}

