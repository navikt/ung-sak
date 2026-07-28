package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.BehandlingÅrsakHelper;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørInnholdBygger;

import java.util.ArrayList;
import java.util.List;


@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class ProgramPeriodeStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørInnholdBygger opphørInnholdBygger;
    private final EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public ProgramPeriodeStrategy(
        OpphørInnholdBygger opphørInnholdBygger,
        EndringProgramPeriodeInnholdBygger endringProgramPeriodeInnholdBygger,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørInnholdBygger = opphørInnholdBygger;
        this.endringProgramPeriodeInnholdBygger = endringProgramPeriodeInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var årsaker = new BehandlingÅrsakHelper(detaljertResultat);

        boolean harEndretStartdato = årsaker.har(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        // En sluttdatoendring er kun reell når programperioden faktisk er lukket. Er den fortsatt åpen, er den
        // gjenåpnet av en opphevelse på samme behandling, og sluttdatoendringen er utdatert (stale) og ignoreres.
        boolean reellSluttdatoendring = årsaker.har(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)
            && UngdomsprogramOpphørUtleder.harLukketProgramperiode(behandling.getId(), ungdomsprogramPeriodeRepository);
        boolean erOpphør = reellSluttdatoendring
            && UngdomsprogramOpphørUtleder.forrigeBehandlingVarLøpende(behandling, ungdomsprogramPeriodeRepository);
        boolean erFlyttetSluttdato = reellSluttdatoendring && !erOpphør;

        var brev = new ArrayList<VedtaksbrevStrategyResultat>();

        if (harEndretStartdato || erFlyttetSluttdato) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.ENDRING_PROGRAMPERIODE, endringProgramPeriodeInnholdBygger,
                "Automatisk brev flytting av: "
                    + (erFlyttetSluttdato ? " sluttdato" : "")
                    + (harEndretStartdato ? " startdato" : "")));
        }

        if (erOpphør) {
            brev.add(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHØR_DOK, opphørInnholdBygger, "Automatisk brev ved opphør."));
        }

        return brev;
    }

}
