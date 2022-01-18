package no.nav.k9.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.inngangsvilkår.InngangsvilkårTjeneste;
import no.nav.k9.sak.kontrakt.opptjening.OverstyringOpptjeningsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringOpptjeningsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class OpptjeningsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringOpptjeningsvilkåretDto> {

    private OpptjeningRepository opptjeningRepository;

    OpptjeningsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningsvilkåretOverstyringshåndterer(OpptjeningRepository opptjeningRepository,
                                                    HistorikkTjenesteAdapter historikkAdapter,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET,
            VilkårType.OPPTJENINGSVILKÅRET,
            inngangsvilkårTjeneste);
        this.opptjeningRepository = opptjeningRepository;
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringOpptjeningsvilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }

    @Override
    protected void precondition(Behandling behandling, OverstyringOpptjeningsvilkåretDto dto) {
        if (dto.getErVilkarOk()) {
            final Optional<Opptjening> opptjening = opptjeningRepository.finnOpptjening(behandling.getId()).flatMap(it -> it.finnOpptjening(dto.getPeriode().getFom()));
            if (opptjening.isPresent()) {
                final long antall = opptjening.get().getOpptjeningAktivitet().stream()
                    .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)).count();
                if (antall > 0) {
                    return;
                }
            }
            throw OverstyringFeil.FACTORY.opptjeningPreconditionFailed().toException();
        }
    }
}
