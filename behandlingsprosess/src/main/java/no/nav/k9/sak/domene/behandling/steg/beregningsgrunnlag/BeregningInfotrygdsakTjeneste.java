package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveSendTilInfotrygdTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;


/** NB! Midlertidlig tjeneste som avslutter sak og oppretter sak i infotrygd.
 *
 * Gjelder ytelsen OSM
 * omsorgspengersaker som faller ut pga. FL(frilans) og SN (selvstendig næringsdrivende)
 */
@ApplicationScoped
public class BeregningInfotrygdsakTjeneste {
    private ProsessTaskRepository prosessTaskRepository;
    private FagsakRepository fagsakRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    protected BeregningInfotrygdsakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BeregningInfotrygdsakTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                         ProsessTaskRepository prosessTaskRepository,
                                         BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                         InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {

        this.prosessTaskRepository = prosessTaskRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public boolean vurderOgOppdaterSakSomBehandlesAvInfotrygd(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst, AbstractLocalDateInterval inntektsperioden) {
        if (ref.getFagsakYtelseType() != FagsakYtelseType.OMSORGSPENGER) {
            throw new IllegalStateException("skal bare benyttes av OMSORGSPENGER, fikk sendt inn " + ref.getFagsakYtelseType());
        }

        if (skalSakenTilInfotrygd(ref, inntektsperioden)) {
            fagsakRepository.fagsakSkalBehandlesAvInfotrygd(ref.getFagsakId());
            dispatchTilInfotrygd(ref);
            oppdaterBeregningsgrunnlagvilkår(kontekst);
            return true;
        }
        return false;
    }

    private boolean skalSakenTilInfotrygd(BehandlingReferanse ref, AbstractLocalDateInterval inntektsperioden) {
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        Optional<AktørArbeid> aktørArbeidFraRegister = inntektArbeidYtelseGrunnlag.getAktørArbeidFraRegister(ref.getAktørId());

        return aktørArbeidFraRegister
                .map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList())
                .stream()
                .anyMatch(yrkesaktivitet -> (yrkesaktivitet.getArbeidType() == ArbeidType.FRILANSER ||
                        yrkesaktivitet.getArbeidType() == ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER ||
                        yrkesaktivitet.getArbeidType() == ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE) &&
                        yrkesaktivitet.getAlleAktivitetsAvtaler()
                                .stream()
                                .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                                .map(AktivitetsAvtale::getPeriode)
                                .anyMatch(datoIntervallEntitet -> datoIntervallEntitet.overlapper(inntektsperioden)));
    }

    private void dispatchTilInfotrygd(BehandlingReferanse ref) {
        ProsessTaskData data = new ProsessTaskData(OpprettOppgaveSendTilInfotrygdTask.TASKTYPE);
        data.setBehandling(ref.getSaksnummer().getVerdi(), String.valueOf(ref.getBehandlingId()), ref.getAktørId().getId());
        prosessTaskRepository.lagre(data);
    }

    private void oppdaterBeregningsgrunnlagvilkår(BehandlingskontrollKontekst kontekst) {
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultatSkalBehandlesIInfotrygd(kontekst);
    }
}
