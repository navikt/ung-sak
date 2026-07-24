package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
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

        boolean harOpphevelseAvOpphør = resultater.innholder(DetaljertResultatType.OPPHØR_OPPHEVET);
        // Ved opphevelse av opphør er en eventuell ENDRING_SLUTTDATO en utdatert (stale) opphør fra
        // race-sammenslåing på samme behandling. Den ignoreres da helt, slik at det verken bestilles opphørsbrev
        // eller blokkeres for forlengelse/opphør ved maksdato-brev som fortsatt kan være aktuelle.
        boolean harEndretSluttdato = !harOpphevelseAvOpphør && resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO);
        boolean harEndretStartdato = resultater.innholder(DetaljertResultatType.ENDRING_STARTDATO);
        boolean erOpphør = harEndretSluttdato && haddeÅpenSluttdatoIForrigeBehandling(behandling, ungdomsprogramPeriodeRepository);
        boolean harFlyttetSluttdato = harEndretSluttdato && !erOpphør;

        var brev = new ArrayList<VedtaksbrevStrategyResultat>();

        if (harEndretStartdato || harFlyttetSluttdato) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.ENDRING_PROGRAMPERIODE, endringProgramPeriodeInnholdBygger,
                "Automatisk brev flytting av: "
                    + (harFlyttetSluttdato ? " sluttdato" : "")
                    + (harEndretStartdato ? " startdato" : "")));
        }

        if (erOpphør) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHØR_DOK, opphørInnholdBygger, "Automatisk brev ved opphør."));
        }

        if (harEndretSluttdato) {
            return brev;
        }

        // Forlengelse, opphør ved maksdato og opphevelse av opphør er kun aktuelt når det ikke samtidig er en
        // reell endring av sluttdato, men kan kombineres med hverandre og gir da hvert sitt brev.
        if (resultater.innholder(DetaljertResultatType.FORLENGET_PERIODE)) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.FORLENGET_PERIODE, forlengetPeriodeInnholdBygger,
                "Automatisk brev ved forlenget periode"));
        }

        if (resultater.innholder(DetaljertResultatType.OPPHØR_VED_MAKSDATO)) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_VED_MAKSDATO_DOK, opphørVedMaksdatoInnholdBygger,
                "Automatisk brev ved opphør grunnet maksdato."));
        }

        if (harOpphevelseAvOpphør) {
            brev.add(opphevelseAvOpphørResultat(behandling));
        }

        return brev;
    }

    /**
     * Opphevelse av opphør: skiller mellom «opphevet» (opphøret ble faktisk vedtatt i en tidligere, avsluttet
     * behandling → eget vedtaksbrev) og «avbrutt i samme behandling» (opphør og opphevelse slått sammen før
     * opphøret rakk å bli vedtatt → intet vedtak å reversere, ingen brev). Skillet avgjøres av
     * {@link UngdomsprogramOpphørUtleder#opphørAvUngdomsprogrammetVarInkludertIVedtaket}.
     * <p>
     * NB: for avbrutt-tilfellet returneres et eksplisitt "ingen brev, årsak IKKE_RELEVANT"-resultat (ikke tom liste),
     * ellers faller perioden på fallback-resultatet IKKE_IMPLEMENTERT og krever manuell "Fatt vedtak".
     */
    private VedtaksbrevStrategyResultat opphevelseAvOpphørResultat(Behandling behandling) {
        if (UngdomsprogramOpphørUtleder.opphørAvUngdomsprogrammetVarInkludertIVedtaket(behandling, ungdomsprogramPeriodeRepository)) {
            return VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_OPPHEVET_DOK, opphørOpphevetInnholdBygger,
                "Automatisk brev ved opphevelse av opphør.");
        }
        return VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,
            "Opphør og opphevelse havnet på samme, fortsatt åpne behandling - opphøret ble aldri vedtatt, ikke behov for vedtaksbrev.");
    }


    private static boolean haddeÅpenSluttdatoIForrigeBehandling(Behandling behandling, UngdomsprogramPeriodeRepository repo) {
        return behandling.getOriginalBehandlingId()
            .flatMap(repo::hentGrunnlag)
            .map(grunnlag -> grunnlag.getUngdomsprogramPerioder().getPerioder().stream()
                .anyMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato())))
            .orElse(false);
    }

}
