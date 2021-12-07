package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.medisinsk;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarILivetsSluttfaseOpplysningerDto;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentHarOppdatertEksisterendeVurderinger;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.vilkår.PLSVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarILivetsSluttfaseOpplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarILivetsSluttfaseOpplysninger implements AksjonspunktOppdaterer<AvklarILivetsSluttfaseOpplysningerDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingRepository behandlingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;


    AvklarILivetsSluttfaseOpplysninger() {
        // for CDI proxy
    }

    @Inject
    public AvklarILivetsSluttfaseOpplysninger(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                              @FagsakYtelseTypeRef("PPN") PLSVilkårsPerioderTilVurderingTjeneste PLSVilkårsPerioderTilVurderingTjeneste,
                                              BehandlingRepository behandlingRepository,
                                              SykdomDokumentRepository sykdomDokumentRepository) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = behandlingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
    }


    @Override
    public OppdateringResultat oppdater(AvklarILivetsSluttfaseOpplysningerDto dto, AksjonspunktOppdaterParameter param) {
        final Behandling behandling = behandlingRepository.hentBehandling(param.getRef().getBehandlingId());

        /*
         * Vi kvitterer her ut alle dokumenter som ligger på pleietrengende uavhengig av om
         * saksbehandler har sett dokumentet eller ikke. En bedre løsning er å enten:
         *
         * 1. Oppdatere behandlingsversjon når det kommer inn nye dokumenter.
         * 2. Sende med fra frontend hvilke dokumenter som skal kvitteres ut.
         *
         * ...men dette har ikke blitt prioritert.
         */
        kvitterUtAlleDokumenterSomLiggerPåPleietrengende(behandling);

        lagHistorikkinnslag(param, "Sykdom manuelt behandlet.");

        final OppdateringResultat resultat = OppdateringResultat.utenTransisjon().medTotrinnHvis(false).build();
        resultat.skalRekjøreSteg();
        resultat.setSteg(BehandlingStegType.VURDER_MEDISINSKVILKÅR);

        return resultat;
    }

    private void kvitterUtAlleDokumenterSomLiggerPåPleietrengende(final Behandling behandling) {
        final LocalDateTime nå = LocalDateTime.now();
        final List<SykdomDokument> dokumenter = sykdomDokumentRepository.hentDokumentSomIkkeHarOppdatertEksisterendeVurderinger(behandling.getFagsak().getPleietrengendeAktørId());
        for (var sykdomDokument : dokumenter) {
            sykdomDokumentRepository.kvitterDokumenterMedOppdatertEksisterendeVurderinger(new SykdomDokumentHarOppdatertEksisterendeVurderinger(sykdomDokument, SubjectHandler.getSubjectHandler().getUid(), nå));
        }
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, String begrunnelse) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_MEDISINSK)
            .medBegrunnelse(begrunnelse);
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }
}
