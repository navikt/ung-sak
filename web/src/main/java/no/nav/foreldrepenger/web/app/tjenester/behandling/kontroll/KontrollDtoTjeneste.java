package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.risikoklassifisering.modell.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.FaresignalGruppeWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.FaresignalWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class KontrollDtoTjeneste {

    private RisikovurderingTjeneste risikovurderingTjeneste;

    KontrollDtoTjeneste() {
        // CDI
    }

    @Inject
    public KontrollDtoTjeneste(RisikovurderingTjeneste risikovurderingTjeneste) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
    }

    public Optional<KontrollresultatDto> lagKontrollresultatForBehandling(Behandling behandling) {
        Optional<RisikoklassifiseringEntitet> risikoklassifiseringEntitet = risikovurderingTjeneste.hentRisikoklassifiseringForBehandling(behandling.getId());

        if (!risikoklassifiseringEntitet.isPresent()) {
            return Optional.of(KontrollresultatDto.ikkeKlassifisert());
        }

        RisikoklassifiseringEntitet entitet = risikoklassifiseringEntitet.get();
        KontrollresultatDto dto = new KontrollresultatDto();
        dto.setKontrollresultat(entitet.getKontrollresultat());

        if (entitet.erHøyrisiko()) {
            Optional<FaresignalWrapper> faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);
            dto.setFaresignalVurdering(entitet.getFaresignalVurdering());
            faresignalWrapper.ifPresent(en -> {
                dto.setIayFaresignaler(lagFaresignalDto(en.getIayFaresignaler()));
                dto.setMedlFaresignaler(lagFaresignalDto(en.getMedlFaresignaler()));
            });
        }

        return Optional.of(dto);
    }

    private FaresignalgruppeDto lagFaresignalDto(FaresignalGruppeWrapper faresignalgruppe) {
        if (faresignalgruppe == null || faresignalgruppe.getFaresignaler().isEmpty()) {
            return null;
        }

        FaresignalgruppeDto dto = new FaresignalgruppeDto();
        dto.setKontrollresultat(faresignalgruppe.getKontrollresultat());
        dto.setFaresignaler(faresignalgruppe.getFaresignaler());

        return dto;
    }

}
