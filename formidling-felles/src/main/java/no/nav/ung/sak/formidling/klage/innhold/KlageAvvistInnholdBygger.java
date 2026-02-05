package no.nav.ung.sak.formidling.klage.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravAdapter;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.template.dto.KlageAvvistDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Dependent
public class KlageAvvistInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(KlageAvvistInnholdBygger.class);
    private KlageRepository klageRepository;

    @Inject
    public KlageAvvistInnholdBygger(KlageRepository klageRepository) {
        this.klageRepository = klageRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        var klageutredning = klageRepository.hentKlageUtredning(behandling.getId());
        var formkrav = klageutredning.getFormkrav().orElseThrow();
        var formkravA = formkrav.tilFormkrav();

        return new TemplateInnholdResultat(TemplateType.KLAGE_AVVIST,
            new KlageAvvistDto(
                "NAV",
                formkrav.hentAvvistÅrsaker().size() > 1,
                !formkravA.isFristOverholdt(),
                !formkravA.isErSignert(),
                !formkravA.gjelderVedtak(),
                !formkravA.isErKlagerPart(),
                !formkravA.isErKonkret(),
                lagHjemmelTekst(formkravA)
            )
        );
    }

    public static String lagHjemmelTekst(KlageFormkravAdapter paragrafBygger) {
        String hjemler = Stream.of(
                HjemmelMapper.lagTekst(paragrafBygger.hentArbeidsmarkedParagrafer(), "arbeidsmarkedsloven"),
                HjemmelMapper.lagTekst(paragrafBygger.hentForvaltningslovParagrafer(), "forvaltningsloven")
            ).filter(Objects::nonNull)
            .collect(Collectors.joining(" og "));

        if (hjemler.isEmpty()) {
            throw new IllegalStateException("Mangler hjemmeltekst");
        }

        return "Vedtaket er gjort etter " + hjemler + ".";
    }
}
