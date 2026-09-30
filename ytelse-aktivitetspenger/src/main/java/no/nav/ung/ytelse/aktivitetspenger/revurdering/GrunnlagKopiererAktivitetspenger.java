package no.nav.ung.ytelse.aktivitetspenger.revurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedSøknadsfaktaGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;

@ApplicationScoped
@FagsakYtelseTypeRef(AKTIVITETSPENGER)
public class GrunnlagKopiererAktivitetspenger implements GrunnlagKopierer {

    private PersonopplysningRepository personopplysningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private UttalelseRepository uttalelseRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private BostedSøknadsfaktaGrunnlagRepository bostedSøknadsfaktaGrunnlagRepository;
    private AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private StartdatoRepository startdatoRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    public GrunnlagKopiererAktivitetspenger() {
        // for CDI proxy
    }

    @Inject
    public GrunnlagKopiererAktivitetspenger(BehandlingRepositoryProvider repositoryProvider,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            TilkjentYtelseRepository tilkjentYtelseRepository,
                                            UttalelseRepository uttalelseRepository,
                                            OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
                                            BostedSøknadsfaktaGrunnlagRepository bostedSøknadsfaktaGrunnlagRepository,
                                            AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
                                            InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository, StartdatoRepository startdatoRepository,
                                            VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository) {
        this.iayTjeneste = iayTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.uttalelseRepository = uttalelseRepository;
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.bostedSøknadsfaktaGrunnlagRepository = bostedSøknadsfaktaGrunnlagRepository;
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.startdatoRepository = startdatoRepository;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
    }


    @Override
    public void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny) {
        Long originalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        forutgåendeMedlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        tilkjentYtelseRepository.kopierKontrollPerioder(originalBehandlingId, nyBehandlingId);
        uttalelseRepository.kopier(originalBehandlingId, nyBehandlingId);
        startdatoRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        bostedSøknadsfaktaGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        // Felles vilkårsavklaringsgrunnlag — kopierer alle vilkårstyper, bl.a. bosteds- og bistandsvilkåret
        vilkårsavklaringGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        aktivitetspengerGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        inngangsvilkårVurderingRepository.kopier(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny) {
        kopierGrunnlagVedManuellOpprettelse(original, ny);
    }

}
