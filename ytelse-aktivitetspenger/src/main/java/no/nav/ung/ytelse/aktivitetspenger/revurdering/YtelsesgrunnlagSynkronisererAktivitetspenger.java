package no.nav.ung.ytelse.aktivitetspenger.revurdering;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.AKTIVITETSPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedSøknadsfaktaGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger.YtelsesgrunnlagSynkroniserer;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;

/**
 * Satsgrunnlaget og grunnlaget for vilkårene i aktivitetspenger. Samme liste som i
 * {@link GrunnlagKopiererAktivitetspenger}, utenom data som er felles for ytelsene.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(AKTIVITETSPENGER)
public class YtelsesgrunnlagSynkronisererAktivitetspenger implements YtelsesgrunnlagSynkroniserer {

    private AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    private BostedSøknadsfaktaGrunnlagRepository bostedSøknadsfaktaGrunnlagRepository;
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    YtelsesgrunnlagSynkronisererAktivitetspenger() {
        // for CDI proxy
    }

    @Inject
    public YtelsesgrunnlagSynkronisererAktivitetspenger(AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
                                                        OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository,
                                                        BostedSøknadsfaktaGrunnlagRepository bostedSøknadsfaktaGrunnlagRepository,
                                                        VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository,
                                                        InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
        this.forutgåendeMedlemskapRepository = forutgåendeMedlemskapRepository;
        this.bostedSøknadsfaktaGrunnlagRepository = bostedSøknadsfaktaGrunnlagRepository;
        this.vilkårsavklaringGrunnlagRepository = vilkårsavklaringGrunnlagRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
    }

    @Override
    public void erstattYtelsesgrunnlagFraOrdinærBehandling(Long fraBehandlingId, Long tilKontrollbehandlingId) {
        aktivitetspengerGrunnlagRepository.erstattGrunnlagMedKopiFra(fraBehandlingId, tilKontrollbehandlingId);
        forutgåendeMedlemskapRepository.erstattGrunnlagMedKopiFra(fraBehandlingId, tilKontrollbehandlingId);
        bostedSøknadsfaktaGrunnlagRepository.erstattGrunnlagMedKopiFra(fraBehandlingId, tilKontrollbehandlingId);
        vilkårsavklaringGrunnlagRepository.erstattGrunnlagMedKopiFra(fraBehandlingId, tilKontrollbehandlingId);
        inngangsvilkårVurderingRepository.erstattGrunnlagMedKopiFra(fraBehandlingId, tilKontrollbehandlingId);
    }
}
