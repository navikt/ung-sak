package no.nav.foreldrepenger.web.app.tjenester.behandling.medisinsk;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsyn;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæringer;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.kontrakt.medisinsk.AvklarMedisinskeOpplysningerDto;
import no.nav.k9.sak.kontrakt.medisinsk.Legeerklæring;
import no.nav.k9.sak.kontrakt.medisinsk.Pleiebehov;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarMedisinskeOpplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarMedisinskeOpplysninger implements AksjonspunktOppdaterer<AvklarMedisinskeOpplysningerDto> {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    AvklarMedisinskeOpplysninger() {
        // for CDI proxy
    }

    @Inject
    AvklarMedisinskeOpplysninger(MedisinskGrunnlagRepository medisinskGrunnlagRepository) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarMedisinskeOpplysningerDto dto, AksjonspunktOppdaterParameter param) {
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(param.getBehandlingId());

        final var legeerklæringer = mapLegeerklæringer(medisinskGrunnlag.map(MedisinskGrunnlag::getLegeerklæringer).orElse(null), dto.getLegeerklæringer());
        final var kontinuerligTilsyn = mapKontinuerligTilsyn(medisinskGrunnlag.map(MedisinskGrunnlag::getKontinuerligTilsyn).orElse(null), dto.getPleiebehov());

        medisinskGrunnlagRepository.lagreOgFlush(param.getBehandling(), kontinuerligTilsyn, legeerklæringer);

        return OppdateringResultat.utenOveropp();
    }

    private KontinuerligTilsyn mapKontinuerligTilsyn(KontinuerligTilsyn kontinuerligTilsyn, Pleiebehov pleiebehov) {
        final var oppdatertKontinuerligTilsyn = new KontinuerligTilsyn(kontinuerligTilsyn);
        // TODO: Build
        return oppdatertKontinuerligTilsyn;
    }

    private Legeerklæringer mapLegeerklæringer(Legeerklæringer legeerklæringer, List<Legeerklæring> dtoLegeerklæringer) {
        final var oppdatertLegeerklæringer = new Legeerklæringer(legeerklæringer);
        // TODO: Build
        return oppdatertLegeerklæringer;
    }
}
