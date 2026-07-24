package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.*;

import java.util.ArrayList;
import java.util.List;


@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class ProgramPeriodeStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørInnholdBygger opphørInnholdBygger;
    private final EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger;
    private final ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger;
    private final OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger;
    private final OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public ProgramPeriodeStrategy(
        OpphørInnholdBygger opphørInnholdBygger,
        EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger,
        ForlengetPeriodeInnholdBygger forlengetPeriodeInnholdBygger,
        OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger,
        OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørInnholdBygger = opphørInnholdBygger;
        this.endringProgramPeriodeInnholdBygger = endringProgramPeriodeInnholdBygger;
        this.forlengetPeriodeInnholdBygger = forlengetPeriodeInnholdBygger;
        this.opphørVedMaksdatoInnholdBygger = opphørVedMaksdatoInnholdBygger;
        this.opphørOpphevetInnholdBygger = opphørOpphevetInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));

        boolean harFjernetOpphør = resultater.innholder(DetaljertResultatType.OPPHØR_OPPHEVET);
        boolean harEndretSluttdato = resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO) ;
        boolean harEndretStartdato = resultater.innholder(DetaljertResultatType.ENDRING_STARTDATO);
        boolean erOpphør = harEndretSluttdato && behandling.getOriginalBehandlingId()
            .map(it -> !harSluttdatoSatt(it, ungdomsprogramPeriodeRepository))
            .orElse(false);
        boolean harFlyttetSluttdato = harEndretSluttdato && !erOpphør;

        boolean erSluttdatoSatt = harSluttdatoSatt(behandling.getId(), ungdomsprogramPeriodeRepository);

        var brev = new ArrayList<VedtaksbrevStrategyResultat>();

        if (harEndretStartdato || (harFlyttetSluttdato && erSluttdatoSatt)) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.ENDRING_PROGRAMPERIODE, endringProgramPeriodeInnholdBygger,
                "Automatisk brev flytting av: "
                    + (harFlyttetSluttdato ? " sluttdato" : "")
                    + (harEndretStartdato ? " startdato" : "")));
        }

        if (erOpphør && erSluttdatoSatt) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHØR_DOK, opphørInnholdBygger, "Automatisk brev ved opphør."));
        }

        if (harEndretSluttdato && erSluttdatoSatt) {
            return brev;
        }

        // Forlengelse, opphør ved maksdato og opphevelse av opphør er kun aktuelt når det ikke samtidig er en
        // reell endring av sluttdato, men kan kombineres med hverandre og gir da hvert sitt brev.
        if (!erSluttdatoSatt && resultater.innholder(DetaljertResultatType.FORLENGET_PERIODE)) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.FORLENGET_PERIODE, forlengetPeriodeInnholdBygger,
                "Automatisk brev ved forlenget periode"));
        }

        if (!erSluttdatoSatt && resultater.innholder(DetaljertResultatType.OPPHØR_VED_MAKSDATO)) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_VED_MAKSDATO_DOK, opphørVedMaksdatoInnholdBygger,
                "Automatisk brev ved opphør grunnet maksdato."));
        }

        if (harFjernetOpphør) {
            Boolean harForrigeBehandlingSluttdatoSatt = behandling.getOriginalBehandlingId()
                .map(it -> harSluttdatoSatt(it, ungdomsprogramPeriodeRepository))
                .orElse(false);

            if (harForrigeBehandlingSluttdatoSatt) {
                if (!erSluttdatoSatt) {
                    brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                        DokumentMalType.OPPHOR_OPPHEVET_DOK, opphørOpphevetInnholdBygger,
                        "Automatisk brev ved opphevelse av opphør."));
                }
            }

            if (brev.isEmpty()) {
                brev.add(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,
                    "Opphør og opphevelse havnet på samme, fortsatt åpne behandling - opphøret ble aldri vedtatt, ikke behov for vedtaksbrev."));
            }

        }

        return brev;
    }


    private static boolean harSluttdatoSatt(Long behandlingId, UngdomsprogramPeriodeRepository repo) {
        return repo.hentGrunnlag(behandlingId)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder().stream()
                .noneMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato())))
            .orElse(true);
    }

}
