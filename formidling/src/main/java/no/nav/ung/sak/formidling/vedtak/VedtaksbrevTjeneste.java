package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrev;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValg;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequest;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Dependent
public class VedtaksbrevTjeneste {

    private final BehandlingRepository behandlingRepository;
    private final VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste;
    private final VedtaksbrevRegler vedtaksbrevRegler;
    private final VedtaksbrevValgRepository vedtaksbrevValgRepository;

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevTjeneste.class);

    @Inject
    public VedtaksbrevTjeneste(
        VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste,
        VedtaksbrevRegler vedtaksbrevRegler,
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

        VedtaksbrevValg førsteValg = vedtaksbrevValg.getFirst();

        return new VedtaksbrevValgResponse(
            true,
            førsteValg.enableHindre(),
            førsteValg.hindret(),
            førsteValg.kanOverstyreHindre(),
            førsteValg.enableRediger(),
            førsteValg.redigert(),
            førsteValg.kanOverstyreRediger(),
            førsteValg.forklaring(),
            førsteValg.redigertBrevHtml(),
            vedtaksbrevValg);
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
            resultat.dokumentMalType(), egenskaper.kanHindre(),
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
            false,
            false,
            false,
            false,
            false,
            false,
            totalResultat.ingenBrevResultater().stream().map(IngenBrev::forklaring).collect(Collectors.joining(", ", "[", "]")),
            null,
            Collections.emptyList()
        );
    }

    public boolean måSkriveBrev(Long behandlingId) {
        var totalResultat = vedtaksbrevRegler.kjør(behandlingId);
        if (!totalResultat.harBrev()) {
            return false;
        }

        return totalResultat.vedtaksbrevResultater().stream()
            .map(Vedtaksbrev::vedtaksbrevEgenskaper)
            .anyMatch(
                egenskaper -> egenskaper.kanRedigere() && !egenskaper.kanOverstyreRediger()
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

        if (!vedtaksbrevEgenskaper.kanRedigere() && dto.redigert() != null) {
            throw new IllegalArgumentException("Brevet kan ikke redigeres.");
        }

        if (!vedtaksbrevEgenskaper.kanHindre() && dto.hindret() != null) {
            throw new IllegalArgumentException("Brevet kan ikke hindres. ");
        }

        vedtaksbrevValgEntitet.setHindret(Boolean.TRUE.equals(dto.hindret()));
        vedtaksbrevValgEntitet.setRedigert(Boolean.TRUE.equals(dto.redigert()));
        vedtaksbrevValgEntitet.rensOgSettRedigertHtml(dto.redigertHtml());
        return vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);

    }

    public List<GenerertBrev> forhåndsvis(VedtaksbrevForhåndsvisRequest dto) {
        List<GenerertBrev> genererteBrev = doForhåndsvis(dto);
        if (genererteBrev.isEmpty()) {
            throw new IllegalArgumentException("Ingen vedtaksbrev generert for behandling. Request: " + dto);
        }
        return genererteBrev;
    }

    private List<GenerertBrev> doForhåndsvis(VedtaksbrevForhåndsvisRequest dto) {
        BehandlingVedtaksbrevResultat totalresultater = vedtaksbrevRegler.kjør(dto.behandlingId());
        validerHarBrev(totalresultater);


        var kunHtml = Boolean.TRUE.equals(dto.htmlVersjon());
        var relevanteVedtaksbrev = totalresultater.vedtaksbrevResultater().stream()
            .filter(v -> dto.dokumentMalType() == null || v.dokumentMalType() == dto.dokumentMalType())
            .toList();

        if (dto.redigertVersjon() == null) {
            return genererFraValg(dto, kunHtml, relevanteVedtaksbrev, totalresultater);
        }

        if (dto.redigertVersjon()) {
            if (dto.dokumentMalType() != null) {
                return List.of(vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), dto.dokumentMalType(), kunHtml));
            }
            return totalresultater.vedtaksbrevResultater().stream()
                .map(it -> vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), it.dokumentMalType(), kunHtml))
                .toList();
        }

        return genererAutomatiskeBrev(dto, relevanteVedtaksbrev, totalresultater, kunHtml);
    }

    private static void validerHarBrev(BehandlingVedtaksbrevResultat totalresultater) {
        if (!totalresultater.harBrev()) {
            throw new IllegalArgumentException("Ingen vedtaksbrev resultater for behandling. Årsak: " + totalresultater.ingenBrevResultater().stream()
                .map(IngenBrev::forklaring)
                .collect(Collectors.joining(", ", "[", "]")));
        }
    }

    private List<GenerertBrev> genererFraValg(VedtaksbrevForhåndsvisRequest dto, boolean kunHtml, List<Vedtaksbrev> relevanteVedtaksbrev, BehandlingVedtaksbrevResultat totalresultater) {
        var relevanteValg = vedtaksbrevValgRepository.finnVedtakbrevValg(dto.behandlingId()).stream()
            .filter(it -> dto.dokumentMalType() == null || it.getDokumentMalType() == dto.dokumentMalType())
            .toList();

        var manuelleBrev = relevanteValg.stream()
            .filter(it -> !it.isHindret())
            .filter(VedtaksbrevValgEntitet::isRedigert)
            .map(it -> vedtaksbrevGenerererTjeneste.genererManuellVedtaksbrev(dto.behandlingId(), it.getDokumentMalType(), kunHtml))
            .toList();

        var redigerteEllerHindredeBrev = relevanteValg.stream()
            .filter(it -> it.isHindret() || it.isRedigert())
            .map(VedtaksbrevValgEntitet::getDokumentMalType)
            .toList();

        var automatiske = relevanteVedtaksbrev.stream()
            .filter(vedtaksbrev -> !redigerteEllerHindredeBrev.contains(vedtaksbrev.dokumentMalType()))
            .toList();

        var automatiskeBrev = genererAutomatiskeBrev(dto, automatiske, totalresultater, kunHtml);

        return Stream.concat(manuelleBrev.stream(), automatiskeBrev.stream()).toList();
    }

    private List<GenerertBrev> genererAutomatiskeBrev(VedtaksbrevForhåndsvisRequest dto, List<Vedtaksbrev> vedtaksbrev, BehandlingVedtaksbrevResultat totalresultater, boolean kunHtml) {
        return vedtaksbrev.stream()
            .map(v -> vedtaksbrevGenerererTjeneste.genererAutomatiskVedtaksbrev(
                new VedtaksbrevGenerererInput(
                    dto.behandlingId(),
                    v,
                    totalresultater.detaljertResultatTimeline(),
                    kunHtml
                )))
            .toList();
    }

    public void ryddVedTilbakeHopp(Long behandlingId) {
        vedtaksbrevValgRepository.finnVedtakbrevValg(behandlingId)
            .forEach(valg -> {
                valg.deaktiver();
                vedtaksbrevValgRepository.lagre(valg);
            });


    }


}

